package com.atguigu.java.ai.langchain4j.bean;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeIngestResult {

    // 请求导入的文件数量
    private int requestedCount;
    // 导入成功数量
    private int successCount;
    // 导入失败数量
    private int failedCount;
    // 导入成功的文件
    private List<String> successFiles = new ArrayList<>();
    // 导入失败的文件
    private List<String> failedFiles = new ArrayList<>();
    // 导入结果说明
    private List<String> messages = new ArrayList<>();

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<String> getSuccessFiles() {
        return successFiles;
    }

    public void setSuccessFiles(List<String> successFiles) {
        this.successFiles = successFiles;
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }

    public void setFailedFiles(List<String> failedFiles) {
        this.failedFiles = failedFiles;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "KnowledgeIngestResult{" +
                "requestedCount=" + requestedCount +
                ", successCount=" + successCount +
                ", failedCount=" + failedCount +
                ", successFiles=" + successFiles +
                ", failedFiles=" + failedFiles +
                ", messages=" + messages +
                '}';
    }
}
