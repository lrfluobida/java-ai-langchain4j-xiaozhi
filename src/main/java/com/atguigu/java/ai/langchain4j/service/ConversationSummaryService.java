package com.atguigu.java.ai.langchain4j.service;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface ConversationSummaryService {

    /**
     * 获取会话摘要
     * @param memoryId 会话id
     * @return 摘要内容，不存在时返回空字符串
     */
    String getSummary(Object memoryId);

    /**
     * 根据当前会话消息刷新摘要
     * @param memoryId 会话id
     * @param messages 当前会话消息列表
     */
    void refreshSummary(Object memoryId, List<ChatMessage> messages);

    /**
     * 删除指定会话的摘要
     * @param memoryId 会话id
     */
    void deleteSummary(Object memoryId);
}
