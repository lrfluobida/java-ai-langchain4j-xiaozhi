package com.atguigu.java.ai.langchain4j.skill.model;

/**
 * 披露策略：根据对话状态决定应该披露到哪个级别
 */
public class DisclosureStrategy {

    private static final int STANDARD_TURN_THRESHOLD = 3;
    private static final int ADVANCED_TURN_THRESHOLD = 6;
    private static final int ADVANCED_TOOL_CALL_THRESHOLD = 2;

    /**
     * 根据会话状态计算应该披露的级别
     */
    public DisclosureLevel calculateLevel(SkillActivation activation) {
        if (activation == null) {
            return DisclosureLevel.BASIC;
        }

        int activeTurns = activation.getActiveTurns();
        int toolCallCount = activation.getToolCallCount();
        boolean hasError = activation.hasError();

        // 遇到错误立即升级到高级
        if (hasError) {
            return DisclosureLevel.ADVANCED;
        }

        // 工具调用次数多，说明场景复杂
        if (toolCallCount >= ADVANCED_TOOL_CALL_THRESHOLD) {
            return DisclosureLevel.ADVANCED;
        }

        // 对话轮次达到阈值，逐步升级
        if (activeTurns >= ADVANCED_TURN_THRESHOLD) {
            return DisclosureLevel.ADVANCED;
        }

        if (activeTurns >= STANDARD_TURN_THRESHOLD) {
            return DisclosureLevel.STANDARD;
        }

        return DisclosureLevel.BASIC;
    }

    /**
     * 强制升级到指定级别（用于显式触发）
     */
    public DisclosureLevel upgradeToLevel(DisclosureLevel current, DisclosureLevel target) {
        if (target.getPriority() > current.getPriority()) {
            return target;
        }
        return current;
    }
}
