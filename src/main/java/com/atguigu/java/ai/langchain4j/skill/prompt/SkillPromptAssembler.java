package com.atguigu.java.ai.langchain4j.skill.prompt;

import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SkillPromptAssembler {

    public String assemble(List<SkillDefinition> skills) {
        if (skills == null || skills.isEmpty()) {
            return "";
        }

        return skills.stream()
                .map(SkillDefinition::getContent)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }
}