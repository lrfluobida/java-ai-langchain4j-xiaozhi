package com.atguigu.java.ai.langchain4j.skill.service;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.DisclosureStrategy;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.prompt.SkillPromptAssembler;
import com.atguigu.java.ai.langchain4j.skill.router.SkillRouter;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionService;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SkillPromptService {

    private final SkillSelectionService skillSelectionService;
    private final SkillPromptAssembler skillPromptAssembler;
    private final SkillSessionStore skillSessionStore;
    private final DisclosureStrategy disclosureStrategy;

    @Autowired
    public SkillPromptService(SkillSelectionService skillSelectionService,
                             SkillPromptAssembler skillPromptAssembler,
                             SkillSessionStore skillSessionStore) {
        this.skillSelectionService = skillSelectionService;
        this.skillPromptAssembler = skillPromptAssembler;
        this.skillSessionStore = skillSessionStore;
        this.disclosureStrategy = new DisclosureStrategy();
    }

    public SkillPromptService(SkillRouter skillRouter,
                             SkillPromptAssembler skillPromptAssembler,
                             SkillSessionStore skillSessionStore) {
        this(new SkillSelectionService(
                        skillRouter.getSkillRegistry(),
                        skillSessionStore,
                        context -> {
                            throw new IllegalStateException("fallback to keyword router");
                        },
                        skillRouter),
                skillPromptAssembler,
                skillSessionStore);
    }

    public SkillPromptService(SkillRouter skillRouter, SkillPromptAssembler skillPromptAssembler) {
        this(skillRouter, skillPromptAssembler, new SkillSessionStore());
    }

    /**
     * 解析 skill 规则（不支持渐进式披露，向后兼容）
     */
    public String resolveSkillRules(Long memoryId, String userMessage) {
        List<SkillDefinition> matchedSkills = skillSelectionService.select(new SkillContext(memoryId, userMessage));
        return skillPromptAssembler.assemble(matchedSkills);
    }

    /**
     * 解析 skill 规则（支持渐进式披露）
     */
    public String resolveSkillRulesWithDisclosure(Long memoryId, String userMessage) {
        List<SkillDefinition> matchedSkills = skillSelectionService.select(new SkillContext(memoryId, userMessage));

        // 获取当前会话的激活状态
        Map<String, SkillActivation> activations = skillSessionStore.getOrCreate(memoryId)
                .getActiveSkills();

        // 更新披露级别
        for (SkillDefinition skill : matchedSkills) {
            if (skill.isProgressive()) {
                SkillActivation activation = activations.get(skill.getName());
                if (activation != null) {
                    DisclosureLevel newLevel = disclosureStrategy.calculateLevel(activation);
                    activation.setCurrentDisclosureLevel(newLevel);
                }
            }
        }

        return skillPromptAssembler.assembleWithDisclosure(matchedSkills, activations);
    }

    /**
     * 记录工具调用（用于触发披露级别升级）
     */
    public void recordToolCall(Long memoryId, String skillName) {
        skillSessionStore.getOrCreate(memoryId)
                .findActiveSkill(skillName)
                .ifPresent(SkillActivation::incrementToolCallCount);
    }

    /**
     * 记录错误（用于触发披露级别升级）
     */
    public void recordError(Long memoryId, String skillName) {
        skillSessionStore.getOrCreate(memoryId)
                .findActiveSkill(skillName)
                .ifPresent(SkillActivation::markError);
    }
}
