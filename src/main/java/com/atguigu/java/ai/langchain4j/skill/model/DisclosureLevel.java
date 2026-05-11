package com.atguigu.java.ai.langchain4j.skill.model;

/**
 * Skill 内容披露级别
 */
public enum DisclosureLevel {
    /**
     * 基础级别：首次触发时注入，包含最核心的指令
     */
    BASIC(1),

    /**
     * 标准级别：对话进行 3-5 轮后注入，包含常规流程
     */
    STANDARD(2),

    /**
     * 高级级别：遇到复杂场景时注入，包含边缘情况处理
     */
    ADVANCED(3);

    private final int priority;

    DisclosureLevel(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 判断当前级别是否应该包含目标级别的内容
     */
    public boolean includes(DisclosureLevel target) {
        return this.priority >= target.priority;
    }
}
