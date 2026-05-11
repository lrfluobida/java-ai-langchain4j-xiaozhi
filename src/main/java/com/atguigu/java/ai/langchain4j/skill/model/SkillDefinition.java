package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.Objects;

public class SkillDefinition {

    private final String name;
    private final String description;
    private final Integer version;
    private final String content;
    private final LayeredSkillContent layeredContent;
    private final SkillRouteConfig routeConfig;

    public SkillDefinition(String name, String description, Integer version, String content,
                          LayeredSkillContent layeredContent, SkillRouteConfig routeConfig) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.content = content != null ? content : "";
        this.layeredContent = layeredContent;
        this.routeConfig = Objects.requireNonNull(routeConfig, "routeConfig must not be null");
    }

    public SkillDefinition(String name, String description, Integer version, String content, SkillRouteConfig routeConfig) {
        this(name, description, version, content, null, routeConfig);
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

    /**
     * 获取原始完整内容（向后兼容）
     */
    public String getContent() {
        return content;
    }

    /**
     * 获取分层内容
     */
    public LayeredSkillContent getLayeredContent() {
        return layeredContent;
    }

    /**
     * 判断是否启用渐进式披露
     */
    public boolean isProgressive() {
        return layeredContent != null && layeredContent.isProgressive();
    }

    /**
     * 根据披露级别获取内容
     */
    public String getContentForLevel(DisclosureLevel level) {
        if (layeredContent != null && layeredContent.isProgressive()) {
            return layeredContent.getContentForLevel(level);
        }
        return content;
    }

    public SkillRouteConfig getRouteConfig() {
        return routeConfig;
    }
}
