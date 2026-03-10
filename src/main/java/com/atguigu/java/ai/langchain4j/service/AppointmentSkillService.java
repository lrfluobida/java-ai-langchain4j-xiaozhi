package com.atguigu.java.ai.langchain4j.service;

public interface AppointmentSkillService {

    /**
     * 根据当前会话决定是否启用预约技能
     * @param memoryId 会话id
     * @param userMessage 用户消息
     * @return 预约技能规则文本，不需要时返回空字符串
     */
    String resolveAppointmentSkill(Long memoryId, String userMessage);
}
