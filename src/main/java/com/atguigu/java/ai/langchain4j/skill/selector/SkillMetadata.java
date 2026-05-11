package com.atguigu.java.ai.langchain4j.skill.selector;

import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;

public record SkillMetadata(
        String name,
        String description,
        Integer version,
        Integer priority,
        Boolean stickySession,
        Integer maxActiveTurns,
        boolean progressive
) {

    public static SkillMetadata from(SkillDefinition skill) {
        SkillRouteConfig routeConfig = skill.getRouteConfig();
        return new SkillMetadata(
                skill.getName(),
                skill.getDescription(),
                skill.getVersion(),
                routeConfig.getPriority(),
                routeConfig.getStickySession(),
                routeConfig.getMaxActiveTurns(),
                skill.isProgressive()
        );
    }
}
