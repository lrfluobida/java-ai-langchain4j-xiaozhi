package com.atguigu.java.ai.langchain4j.bean;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("conversation_progress")
public class ConversationProgress {

    @Id
    private ObjectId progressId;
    private String memoryId;
    private Integer totalUserMessages;
    private Integer summarizedUserMessages;
    private Integer summaryVersion;
    private String lastUserTurnSignature;
    private Long updatedAt;

    public ObjectId getProgressId() {
        return progressId;
    }

    public void setProgressId(ObjectId progressId) {
        this.progressId = progressId;
    }

    public String getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(String memoryId) {
        this.memoryId = memoryId;
    }

    public Integer getTotalUserMessages() {
        return totalUserMessages;
    }

    public void setTotalUserMessages(Integer totalUserMessages) {
        this.totalUserMessages = totalUserMessages;
    }

    public Integer getSummarizedUserMessages() {
        return summarizedUserMessages;
    }

    public void setSummarizedUserMessages(Integer summarizedUserMessages) {
        this.summarizedUserMessages = summarizedUserMessages;
    }

    public Integer getSummaryVersion() {
        return summaryVersion;
    }

    public void setSummaryVersion(Integer summaryVersion) {
        this.summaryVersion = summaryVersion;
    }

    public String getLastUserTurnSignature() {
        return lastUserTurnSignature;
    }

    public void setLastUserTurnSignature(String lastUserTurnSignature) {
        this.lastUserTurnSignature = lastUserTurnSignature;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}