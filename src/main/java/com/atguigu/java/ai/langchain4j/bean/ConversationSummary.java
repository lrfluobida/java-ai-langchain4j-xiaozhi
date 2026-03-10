package com.atguigu.java.ai.langchain4j.bean;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("conversation_summaries")
public class ConversationSummary {

    @Id
    private ObjectId summaryId;
    // 会话id
    private String memoryId;
    // 会话摘要
    private String summary;
    // 生成摘要时对应的用户消息数量
    private Integer messageCount;
    // 更新时间
    private Long updatedAt;

    public ObjectId getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(ObjectId summaryId) {
        this.summaryId = summaryId;
    }

    public String getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
