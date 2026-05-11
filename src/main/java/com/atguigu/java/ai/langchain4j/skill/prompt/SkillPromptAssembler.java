package com.atguigu.java.ai.langchain4j.skill.prompt;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SkillPromptAssembler {

    /**
     * 组装 skill prompt（不支持渐进式披露，向后兼容）
     */
    public String assemble(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }

        return skills.stream()
                .map(SkillDefinition::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 组装 skill prompt（支持渐进式披露）
     */
    public String assembleWithDisclosure(List<SkillDefinition> skills, Map<String, SkillActivation> activations) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }

        return skills.stream()
                .map(skill -> {
                    if (!skill.isProgressive()) {
                        // 非渐进式 skill，返回完整内容
                        return skill.getContent();
                    }

                    // 渐进式 skill，根据激活状态决定披露级别
                    SkillActivation activation = activations != null ? activations.get(skill.getName()) : null;
                    DisclosureLevel level = activation != null
                            ? activation.getCurrentDisclosureLevel()
                            : DisclosureLevel.BASIC;

                    return skill.getContentForLevel(level);
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }
}