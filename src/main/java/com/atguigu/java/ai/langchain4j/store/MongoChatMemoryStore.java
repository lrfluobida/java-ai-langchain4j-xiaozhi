package com.atguigu.java.ai.langchain4j.store;

import com.atguigu.java.ai.langchain4j.bean.MyChatMessages;
import com.atguigu.java.ai.langchain4j.service.ConversationSummaryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class MongoChatMemoryStore implements ChatMemoryStore {

    private final Map<String, SystemMessage> transientSystemMessages = new ConcurrentHashMap<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ConversationSummaryService conversationSummaryService;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> persistentMessages = loadPersistentMessages(memoryId);
        SystemMessage systemMessage = transientSystemMessages.get(normalizeMemoryId(memoryId));
        if (systemMessage == null) {
            return persistentMessages;
        }

        List<ChatMessage> messages = new ArrayList<>(persistentMessages.size() + 1);
        messages.add(systemMessage);
        messages.addAll(persistentMessages);
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        cacheSystemMessage(memoryId, messages);

        List<ChatMessage> persistentMessages = filterPersistentMessages(messages);
        if (CollectionUtils.isEmpty(persistentMessages)) {
            return;
        }

        Update update = new Update();
        update.set("memoryId", normalizeMemoryId(memoryId));
        update.set("content", ChatMessageSerializer.messagesToJson(persistentMessages));

        mongoTemplate.upsert(queryByMemoryId(memoryId), update, MyChatMessages.class);
        conversationSummaryService.refreshSummary(memoryId, persistentMessages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        transientSystemMessages.remove(normalizeMemoryId(memoryId));
        mongoTemplate.remove(queryByMemoryId(memoryId), MyChatMessages.class);
        conversationSummaryService.deleteSummary(memoryId);
    }

    private List<ChatMessage> loadPersistentMessages(Object memoryId) {
        MyChatMessages chatMessages = mongoTemplate.findOne(queryByMemoryId(memoryId), MyChatMessages.class);
        if (chatMessages == null || !StringUtils.hasText(chatMessages.getContent())) {
            return new LinkedList<>();
        }
        return ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
    }

    private void cacheSystemMessage(Object memoryId, List<ChatMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        SystemMessage systemMessage = SystemMessage.findLast(messages).orElse(null);
        if (systemMessage != null) {
            transientSystemMessages.put(normalizeMemoryId(memoryId), systemMessage);
        }
    }

    private List<ChatMessage> filterPersistentMessages(List<ChatMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return List.of();
        }
        return messages.stream()
                .filter(this::shouldPersist)
                .collect(Collectors.toList());
    }

    private boolean shouldPersist(ChatMessage message) {
        return message instanceof UserMessage
                || message instanceof AiMessage
                || message instanceof ToolExecutionResultMessage;
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
}