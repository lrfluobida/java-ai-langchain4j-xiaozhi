package com.atguigu.java.ai.langchain4j.skill.registry;

import com.atguigu.java.ai.langchain4j.skill.loader.SkillLoader;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SkillRegistry {

    private final List<SkillDefinition> skills;
    private final Map<String, SkillDefinition> skillsByName;

    public SkillRegistry(SkillLoader skillLoader) {
        Objects.requireNonNull(skillLoader, "skillLoader must not be null");
        this.skills = List.copyOf(skillLoader.loadSkills());
        this.skillsByName = this.skills.stream().collect(Collectors.toUnmodifiableMap(SkillDefinition::getName, Function.identity()));
    }

    public Optional<SkillDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(skillsByName.get(name));
    }

    public List<SkillDefinition> getEnabledSkills() {
        return skills.stream()
                .filter(skill -> Boolean.TRUE.equals(skill.getRouteConfig().getEnabled()))
                .sorted(Comparator.comparingInt((SkillDefinition skill) -> skill.getRouteConfig().getPriority()).reversed()
                        .thenComparing(SkillDefinition::getName))
                .toList();
    }
}