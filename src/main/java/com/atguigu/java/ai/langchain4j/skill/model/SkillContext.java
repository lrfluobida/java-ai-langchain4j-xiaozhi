package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.Objects;

public record SkillContext(Long memoryId, String message) {

    public SkillContext {
        memoryId = Objects.requireNonNull(memoryId, "memoryId must not be null");
        message = message == null ? "" : message;
    }
}