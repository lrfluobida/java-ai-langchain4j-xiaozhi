package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.skill.debug.SkillDebugHelper;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.service.SkillPromptService;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill 调试接口：用于验证 skill 是否生效
 */
@Tag(name = "Skill 调试")
@RestController
@RequestMapping("/debug/skills")
public class SkillDebugController {

    private final SkillPromptService skillPromptService;
    private final SkillSessionStore skillSessionStore;
    private final SkillDebugHelper debugHelper;

    public SkillDebugController(SkillPromptService skillPromptService,
                               SkillSessionStore skillSessionStore,
                               SkillDebugHelper debugHelper) {
        this.skillPromptService = skillPromptService;
        this.skillSessionStore = skillSessionStore;
        this.debugHelper = debugHelper;
    }

    @Operation(summary = "查看当前会话的 skill 状态")
    @GetMapping("/status/{memoryId}")
    public Map<String, Object> getSkillStatus(@PathVariable Long memoryId) {
        var sessionState = skillSessionStore.getOrCreate(memoryId);
        List<String> activeSkills = sessionState.getActiveSkillNames();

        Map<String, Object> result = new HashMap<>();
        result.put("memoryId", memoryId);
        result.put("activeSkillCount", activeSkills.size());
        result.put("activeSkills", activeSkills);

        Map<String, Map<String, Object>> skillDetails = new HashMap<>();
        for (String skillName : activeSkills) {
            sessionState.findActiveSkill(skillName).ifPresent(activation -> {
                Map<String, Object> details = new HashMap<>();
                details.put("activeTurns", activation.getActiveTurns());
                details.put("remainingTurns", activation.getRemainingTurns());
                details.put("toolCallCount", activation.getToolCallCount());
                details.put("hasError", activation.hasError());
                details.put("disclosureLevel", activation.getCurrentDisclosureLevel().name());
                skillDetails.put(skillName, details);
            });
        }
        result.put("skillDetails", skillDetails);

        return result;
    }

    @Operation(summary = "预览指定消息会触发的 skill 内容")
    @PostMapping("/preview")
    public Map<String, Object> previewSkillRules(@RequestBody PreviewRequest request) {
        String basicRules = skillPromptService.resolveSkillRules(
            request.memoryId(),
            request.message()
        );

        String progressiveRules = skillPromptService.resolveSkillRulesWithDisclosure(
            request.memoryId(),
            request.message()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("memoryId", request.memoryId());
        result.put("message", request.message());
        result.put("basicRulesLength", basicRules.length());
        result.put("progressiveRulesLength", progressiveRules.length());
        result.put("basicRules", basicRules);
        result.put("progressiveRules", progressiveRules);
        result.put("isDifferent", !basicRules.equals(progressiveRules));

        return result;
    }

    @Operation(summary = "对比有无 skill 的差异")
    @PostMapping("/compare")
    public Map<String, Object> compareWithAndWithoutSkill(@RequestBody PreviewRequest request) {
        var comparison = debugHelper.compareWithAndWithoutSkill(
            request.memoryId(),
            request.message()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("memoryId", request.memoryId());
        result.put("message", request.message());
        result.put("withSkillLength", comparison.withSkillLength());
        result.put("withoutSkillLength", comparison.withoutSkillLength());
        result.put("difference", comparison.difference());
        result.put("isDifferent", comparison.isDifferent());
        result.put("skillIsActive", comparison.isDifferent());

        return result;
    }

    @Operation(summary = "模拟工具调用（用于测试披露级别升级）")
    @PostMapping("/simulate-tool-call")
    public Map<String, Object> simulateToolCall(@RequestBody ToolCallRequest request) {
        skillPromptService.recordToolCall(request.memoryId(), request.skillName());

        return Map.of(
            "success", true,
            "message", "Tool call recorded for skill: " + request.skillName(),
            "status", getSkillStatus(request.memoryId())
        );
    }

    @Operation(summary = "模拟错误（用于测试披露级别升级）")
    @PostMapping("/simulate-error")
    public Map<String, Object> simulateError(@RequestBody ToolCallRequest request) {
        skillPromptService.recordError(request.memoryId(), request.skillName());

        return Map.of(
            "success", true,
            "message", "Error recorded for skill: " + request.skillName(),
            "status", getSkillStatus(request.memoryId())
        );
    }

    @Operation(summary = "清除会话的 skill 状态")
    @DeleteMapping("/clear/{memoryId}")
    public Map<String, Object> clearSkillSession(@PathVariable Long memoryId) {
        var sessionState = skillSessionStore.getOrCreate(memoryId);
        List<String> activeSkills = sessionState.getActiveSkillNames();

        activeSkills.forEach(sessionState::deactivate);

        return Map.of(
            "success", true,
            "message", "Cleared " + activeSkills.size() + " active skills",
            "clearedSkills", activeSkills
        );
    }

    public static class PreviewRequest {
        private Long memoryId;
        private String message;

        public PreviewRequest() {}

        public PreviewRequest(Long memoryId, String message) {
            this.memoryId = memoryId;
            this.message = message;
        }

        public Long memoryId() { return memoryId; }
        public String message() { return message; }

        public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ToolCallRequest {
        private Long memoryId;
        private String skillName;

        public ToolCallRequest() {}

        public ToolCallRequest(Long memoryId, String skillName) {
            this.memoryId = memoryId;
            this.skillName = skillName;
        }

        public Long memoryId() { return memoryId; }
        public String skillName() { return skillName; }

        public void setMemoryId(Long memoryId) { this.memoryId = memoryId; }
        public void setSkillName(String skillName) { this.skillName = skillName; }
    }
}
