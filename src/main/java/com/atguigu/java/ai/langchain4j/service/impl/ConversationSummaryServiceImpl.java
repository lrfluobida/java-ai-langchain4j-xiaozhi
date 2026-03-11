package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.bean.ConversationProgress;
import com.atguigu.java.ai.langchain4j.bean.ConversationSummary;
import com.atguigu.java.ai.langchain4j.service.ConversationSummaryService;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationSummaryServiceImpl.class);

    private static final int MIN_SUMMARY_USER_MESSAGES = 3;
    private static final int SUMMARY_REFRESH_USER_MESSAGES = 2;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QwenChatModel qwenChatModel;

    @Override
    public String getSummary(Object memoryId) {
        if (memoryId == null) {
            return "";
        }

        ConversationSummary conversationSummary = findSummaryByMemoryId(memoryId);
        if (conversationSummary == null || !StringUtils.hasText(conversationSummary.getSummary())) {
            return "";
        }
        return conversationSummary.getSummary();
    }

    @Override
    public void refreshSummary(Object memoryId, List<ChatMessage> messages) {
        if (memoryId == null || CollectionUtils.isEmpty(messages)) {
            return;
        }

        List<ChatMessage> conversationMessages = filterConversationMessages(messages);
        if (CollectionUtils.isEmpty(conversationMessages)) {
            return;
        }

        ConversationProgress progress = updateProgress(memoryId, conversationMessages);
        ChatMessage latestMessage = conversationMessages.get(conversationMessages.size() - 1);
        if (!(latestMessage instanceof AiMessage)) {
            return;
        }

        int totalUserMessages = defaultValue(progress.getTotalUserMessages());
        int summarizedUserMessages = defaultValue(progress.getSummarizedUserMessages());
        if (totalUserMessages < MIN_SUMMARY_USER_MESSAGES) {
            return;
        }
        if (totalUserMessages - summarizedUserMessages < SUMMARY_REFRESH_USER_MESSAGES) {
            return;
        }

        ConversationSummary existingSummary = findSummaryByMemoryId(memoryId);
        try {
            String summaryPrompt = buildSummaryPrompt(existingSummary, conversationMessages);
            String summary = qwenChatModel.chat(summaryPrompt);
            if (!StringUtils.hasText(summary)) {
                return;
            }

            saveSummary(memoryId, summary, totalUserMessages);
            progress.setSummarizedUserMessages(totalUserMessages);
            progress.setSummaryVersion(defaultValue(progress.getSummaryVersion()) + 1);
            progress.setUpdatedAt(System.currentTimeMillis());
            saveProgress(progress);
        } catch (Exception e) {
            log.warn("Failed to refresh conversation summary, memoryId={}", normalizeMemoryId(memoryId), e);
        }
    }

    @Override
    public void deleteSummary(Object memoryId) {
        if (memoryId == null) {
            return;
        }
        Query query = queryByMemoryId(memoryId);
        mongoTemplate.remove(query, ConversationSummary.class);
        mongoTemplate.remove(query, ConversationProgress.class);
    }

    private ConversationProgress updateProgress(Object memoryId, List<ChatMessage> messages) {
        ConversationProgress progress = findProgressByMemoryId(memoryId);
        if (progress == null) {
            progress = bootstrapProgress(memoryId, messages);
            saveProgress(progress);
            return progress;
        }

        progress.setMemoryId(normalizeMemoryId(memoryId));
        progress.setUpdatedAt(System.currentTimeMillis());

        if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof UserMessage) {
            String currentTurnSignature = buildMessageSignature(messages);
            if (!currentTurnSignature.equals(progress.getLastUserTurnSignature())) {
                progress.setTotalUserMessages(defaultValue(progress.getTotalUserMessages()) + 1);
                progress.setLastUserTurnSignature(currentTurnSignature);
            }
        }

        saveProgress(progress);
        return progress;
    }

    private ConversationProgress bootstrapProgress(Object memoryId, List<ChatMessage> messages) {
        ConversationSummary existingSummary = findSummaryByMemoryId(memoryId);
        ConversationProgress progress = new ConversationProgress();
        progress.setMemoryId(normalizeMemoryId(memoryId));
        progress.setUpdatedAt(System.currentTimeMillis());

        int currentUserMessages = countUserMessages(messages);
        int summarizedUserMessages = existingSummary != null && existingSummary.getMessageCount() != null
                ? existingSummary.getMessageCount()
                : 0;

        progress.setTotalUserMessages(Math.max(currentUserMessages, summarizedUserMessages));
        progress.setSummarizedUserMessages(summarizedUserMessages);
        progress.setSummaryVersion(existingSummary != null && StringUtils.hasText(existingSummary.getSummary()) ? 1 : 0);
        if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof UserMessage) {
            progress.setLastUserTurnSignature(buildMessageSignature(messages));
        }
        return progress;
    }

    private ConversationSummary findSummaryByMemoryId(Object memoryId) {
        return mongoTemplate.findOne(queryByMemoryId(memoryId), ConversationSummary.class);
    }

    private ConversationProgress findProgressByMemoryId(Object memoryId) {
        return mongoTemplate.findOne(queryByMemoryId(memoryId), ConversationProgress.class);
    }

    private void saveSummary(Object memoryId, String summary, int messageCount) {
        Update update = new Update();
        update.set("memoryId", normalizeMemoryId(memoryId));
        update.set("summary", summary);
        update.set("messageCount", messageCount);
        update.set("updatedAt", System.currentTimeMillis());
        mongoTemplate.upsert(queryByMemoryId(memoryId), update, ConversationSummary.class);
    }

    private void saveProgress(ConversationProgress progress) {
        Update update = new Update();
        update.set("memoryId", progress.getMemoryId());
        update.set("totalUserMessages", defaultValue(progress.getTotalUserMessages()));
        update.set("summarizedUserMessages", defaultValue(progress.getSummarizedUserMessages()));
        update.set("summaryVersion", defaultValue(progress.getSummaryVersion()));
        update.set("lastUserTurnSignature", progress.getLastUserTurnSignature());
        update.set("updatedAt", progress.getUpdatedAt());
        mongoTemplate.upsert(queryByMemoryId(progress.getMemoryId()), update, ConversationProgress.class);
    }

    private String buildSummaryPrompt(ConversationSummary existingSummary, List<ChatMessage> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Please summarize the conversation context below for future turns in Simplified Chinese.\n");
        promptBuilder.append("Requirements:\n");
        promptBuilder.append("1. Keep only facts that are useful for later medical support conversations.\n");
        promptBuilder.append("2. Focus on user profile, symptoms, confirmed appointments, preferences, and unfinished tasks.\n");
        promptBuilder.append("3. Do not invent facts that do not appear in the conversation.\n");
        promptBuilder.append("4. Keep the summary concise and under 200 Chinese characters when possible.\n\n");

        if (existingSummary != null && StringUtils.hasText(existingSummary.getSummary())) {
            promptBuilder.append("Existing summary:\n");
            promptBuilder.append(existingSummary.getSummary()).append("\n\n");
        }

        promptBuilder.append("Latest conversation messages:\n");
        promptBuilder.append(ChatMessageSerializer.messagesToJson(messages));
        return promptBuilder.toString();
    }

    private List<ChatMessage> filterConversationMessages(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> message instanceof UserMessage || message instanceof AiMessage)
                .collect(Collectors.toList());
    }

    private int countUserMessages(List<ChatMessage> messages) {
        return (int) messages.stream()
                .filter(UserMessage.class::isInstance)
                .count();
    }

    private Query queryByMemoryId(Object memoryId) {
        String normalizedMemoryId = normalizeMemoryId(memoryId);
        Criteria criteria;
        if (memoryId instanceof String) {
            criteria = Criteria.where("memoryId").is(normalizedMemoryId);
        } else {
            criteria = new Criteria().orOperator(
                    Criteria.where("memoryId").is(normalizedMemoryId),
                    Criteria.where("memoryId").is(memoryId)
            );
        }
        return new Query(criteria);
    }

    private String normalizeMemoryId(Object memoryId) {
        return memoryId == null ? "" : String.valueOf(memoryId);
    }

    private int defaultValue(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildMessageSignature(List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}