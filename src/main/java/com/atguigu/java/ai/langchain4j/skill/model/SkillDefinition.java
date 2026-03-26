package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.Objects;

public class SkillDefinition {

    private final String name;
    private final String description;
    private final Integer version;
    private final String content;
    private final SkillRouteConfig routeConfig;

    public SkillDefinition(String name, String description, Integer version, String content, SkillRouteConfig routeConfig) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.content = Objects.requireNonNull(content, "content must not be null");
        this.routeConfig = Objects.requireNonNull(routeConfig, "routeConfig must not be null");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public SkillRouteConfig getRouteConfig() {
        return routeConfig;
    }
}