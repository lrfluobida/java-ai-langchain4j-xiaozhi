package com.atguigu.java.ai.langchain4j.service.impl;

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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationSummaryServiceImpl.class);

    // 至少累计到一定用户轮次后再开始生成摘要，避免前几轮对话就频繁压缩
    private static final int MIN_SUMMARY_USER_MESSAGES = 3;
    // 与上次摘要相比，新增这么多条用户消息后再刷新一次摘要
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

        ConversationSummary conversationSummary = findByMemoryId(memoryId);
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

        // 摘要时忽略 SystemMessage，只保留真正参与对话的用户消息和 AI 回复
        List<ChatMessage> summaryMessages = filterSummaryMessages(messages);
        if (CollectionUtils.isEmpty(summaryMessages)) {
            return;
        }
        // 只在一轮结束后刷新摘要，也就是最后一条有效消息必须是 AI 回复
        if (!(summaryMessages.get(summaryMessages.size() - 1) instanceof AiMessage)) {
            return;
        }

        int userMessageCount = countUserMessages(summaryMessages);
        if (userMessageCount < MIN_SUMMARY_USER_MESSAGES) {
            return;
        }

        ConversationSummary existingSummary = findByMemoryId(memoryId);
        if (existingSummary != null
                && existingSummary.getMessageCount() != null
                && userMessageCount - existingSummary.getMessageCount() < SUMMARY_REFRESH_USER_MESSAGES) {
            return;
        }

        try {
            String summaryPrompt = buildSummaryPrompt(existingSummary, summaryMessages);
            String summary = qwenChatModel.chat(summaryPrompt);
            if (!StringUtils.hasText(summary)) {
                return;
            }
            saveSummary(memoryId, summary, userMessageCount);
        } catch (Exception e) {
            // 摘要生成失败时不影响主聊天流程，只记录日志方便排查
            log.warn("刷新会话摘要失败，memoryId={}", memoryId, e);
        }
    }

    @Override
    public void deleteSummary(Object memoryId) {
        if (memoryId == null) {
            return;
        }
        Criteria criteria = Criteria.where("memoryId").is(String.valueOf(memoryId));
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ConversationSummary.class);
    }

    private ConversationSummary findByMemoryId(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(String.valueOf(memoryId));
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, ConversationSummary.class);
    }

    private void saveSummary(Object memoryId, String summary, int messageCount) {
        Criteria criteria = Criteria.where("memoryId").is(String.valueOf(memoryId));
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("memoryId", String.valueOf(memoryId));
        // 这里记录的是生成摘要时对应的用户消息数量，用来判断后续是否需要再次刷新摘要
        update.set("messageCount", messageCount);
        update.set("summary", summary);
        update.set("updatedAt", System.currentTimeMillis());
        mongoTemplate.upsert(query, update, ConversationSummary.class);
    }

    private String buildSummaryPrompt(ConversationSummary existingSummary, List<ChatMessage> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据以下会话内容生成一段后续对话可复用的上下文摘要。\n");
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 只保留对后续对话有价值的信息。\n");
        promptBuilder.append("2. 重点保留：用户身份信息、症状描述、就诊偏好、已确认的预约信息、当前待办事项。\n");
        promptBuilder.append("3. 不要编造没有出现过的信息。\n");
        promptBuilder.append("4. 使用简洁中文，控制在200字以内。\n\n");

        if (existingSummary != null && StringUtils.hasText(existingSummary.getSummary())) {
            promptBuilder.append("历史摘要：\n");
            promptBuilder.append(existingSummary.getSummary()).append("\n\n");
        }

        promptBuilder.append("最新对话内容：\n");
        promptBuilder.append(ChatMessageSerializer.messagesToJson(messages));
        return promptBuilder.toString();
    }

    private List<ChatMessage> filterSummaryMessages(List<ChatMessage> messages) {
        return messages.stream()
                .filter(message -> message instanceof UserMessage || message instanceof AiMessage)
                .collect(Collectors.toList());
    }

    private int countUserMessages(List<ChatMessage> messages) {
        return (int) messages.stream()
                .filter(UserMessage.class::isInstance)
                .count();
    }
}
