package com.atguigu.java.ai.langchain4j.skill.selector;

import java.util.List;

public record SkillSelectionResult(
        List<String> selectedSkills,
        List<String> deactivatedSkills,
        Double confidence,
        String reason
) {

    public List<String> selectedSkills() {
        return selectedSkills != null ? selectedSkills : List.of();
    }

    public List<String> deactivatedSkills() {
        return deactivatedSkills != null ? deactivatedSkills : List.of();
    }
}
