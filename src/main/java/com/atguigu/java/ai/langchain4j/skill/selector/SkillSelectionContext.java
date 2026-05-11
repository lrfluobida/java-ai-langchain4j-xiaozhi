package com.atguigu.java.ai.langchain4j.skill.selector;

import java.util.List;

public record SkillSelectionContext(
        Long memoryId,
        String userMessage,
        List<String> activeSkillNames,
        List<SkillMetadata> availableSkills
) {
}
