package com.atguigu.java.ai.langchain4j.store;

import com.atguigu.java.ai.langchain4j.bean.MyChatMessages;
import com.atguigu.java.ai.langchain4j.service.ConversationSummaryService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class MongoChatMemoryStore implements ChatMemoryStore {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ConversationSummaryService conversationSummaryService;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        MyChatMessages myChatMessages = mongoTemplate.findOne(query, MyChatMessages.class);
        if (myChatMessages != null) {
            return ChatMessageDeserializer.messagesFromJson(myChatMessages.getContent());
        }
        return new LinkedList<>();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", ChatMessageSerializer.messagesToJson(list));

        // 根据 query 条件能查询出文档，则修改文档；否则新增文档
        mongoTemplate.upsert(query, update, MyChatMessages.class);
        // 聊天记录更新后，按需刷新会话摘要
        conversationSummaryService.refreshSummary(memoryId, list);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, MyChatMessages.class);
        // 删除聊天记录时，同步删除对应摘要
        conversationSummaryService.deleteSummary(memoryId);
    }
}
