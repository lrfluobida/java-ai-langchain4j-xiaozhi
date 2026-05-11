package com.atguigu.java.ai.langchain4j.skill.debug;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.service.SkillPromptService;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill 调试工具：用于验证 skill 是否生效
 */
@Component
public class SkillDebugHelper {

    private static final Logger log = LoggerFactory.getLogger(SkillDebugHelper.class);

    private final SkillPromptService skillPromptService;
    private final SkillSessionStore skillSessionStore;

    public SkillDebugHelper(SkillPromptService skillPromptService, SkillSessionStore skillSessionStore) {
        this.skillPromptService = skillPromptService;
        this.skillSessionStore = skillSessionStore;
    }

    /**
     * 打印当前会话的 skill 状态
     */
    public void logSkillStatus(Long memoryId, String userMessage) {
        log.info("=== Skill Debug Info ===");
        log.info("Memory ID: {}", memoryId);
        log.info("User Message: {}", userMessage);

        // 获取激活的 skills
        var sessionState = skillSessionStore.getOrCreate(memoryId);
        List<String> activeSkills = sessionState.getActiveSkillNames();

        if (activeSkills.isEmpty()) {
            log.info("No active skills");
        } else {
            log.info("Active skills: {}", activeSkills);

            // 打印每个 skill 的详细状态
            for (String skillName : activeSkills) {
                sessionState.findActiveSkill(skillName).ifPresent(activation -> {
                    log.info("  - Skill: {}", skillName);
                    log.info("    Active turns: {}", activation.getActiveTurns());
                    log.info("    Remaining turns: {}", activation.getRemainingTurns());
                    log.info("    Tool calls: {}", activation.getToolCallCount());
                    log.info("    Has error: {}", activation.hasError());
                    log.info("    Disclosure level: {}", activation.getCurrentDisclosureLevel());
                });
            }
        }

        // 获取将要注入的 skill 内容
        String skillRules = skillPromptService.resolveSkillRules(memoryId, userMessage);
        log.info("Skill rules length: {} chars", skillRules.length());

        if (!skillRules.isEmpty()) {
            log.info("Skill rules preview (first 200 chars):");
            log.info("{}", skillRules.substring(0, Math.min(200, skillRules.length())));
        }

        log.info("========================");
    }

    /**
     * 打印渐进式披露的详细信息
     */
    public void logProgressiveDisclosure(Long memoryId, String userMessage) {
        log.info("=== Progressive Disclosure Debug ===");

        String basicRules = skillPromptService.resolveSkillRules(memoryId, userMessage);
        String progressiveRules = skillPromptService.resolveSkillRulesWithDisclosure(memoryId, userMessage);

        log.info("Basic rules length: {} chars", basicRules.length());
        log.info("Progressive rules length: {} chars", progressiveRules.length());
        log.info("Difference: {} chars", Math.abs(basicRules.length() - progressiveRules.length()));

        var sessionState = skillSessionStore.getOrCreate(memoryId);
        sessionState.getActiveSkillNames().forEach(skillName -> {
            sessionState.findActiveSkill(skillName).ifPresent(activation -> {
                log.info("Skill '{}' disclosure level: {}", skillName, activation.getCurrentDisclosureLevel());
            });
        });

        log.info("====================================");
    }

    /**
     * 对比有无 skill 的差异
     */
    public SkillComparisonResult compareWithAndWithoutSkill(Long memoryId, String userMessage) {
        // 临时禁用 skill
        var sessionState = skillSessionStore.getOrCreate(memoryId);
        List<String> originalActiveSkills = sessionState.getActiveSkillNames();

        // 清空激活状态
        originalActiveSkills.forEach(sessionState::deactivate);
        String withoutSkill = skillPromptService.resolveSkillRules(memoryId, userMessage);

        // 恢复激活状态（重新路由）
        String withSkill = skillPromptService.resolveSkillRules(memoryId, userMessage);

        return new SkillComparisonResult(
            withSkill.length(),
            withoutSkill.length(),
            withSkill.length() - withoutSkill.length(),
            !withSkill.equals(withoutSkill)
        );
    }

    public static class SkillComparisonResult {
        private final int withSkillLength;
        private final int withoutSkillLength;
        private final int difference;
        private final boolean isDifferent;

        public SkillComparisonResult(int withSkillLength, int withoutSkillLength, int difference, boolean isDifferent) {
            this.withSkillLength = withSkillLength;
            this.withoutSkillLength = withoutSkillLength;
            this.difference = difference;
            this.isDifferent = isDifferent;
        }

        public int withSkillLength() { return withSkillLength; }
        public int withoutSkillLength() { return withoutSkillLength; }
        public int difference() { return difference; }
        public boolean isDifferent() { return isDifferent; }

        @Override
        public String toString() {
            return String.format(
                "With skill: %d chars, Without skill: %d chars, Difference: %d chars, Is different: %s",
                withSkillLength, withoutSkillLength, difference, isDifferent
            );
        }
    }
}
