package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SkillSessionState {

    private final Long memoryId;
    private final Map<String, SkillActivation> activeSkills = new LinkedHashMap<>();

    public SkillSessionState(Long memoryId) {
        this.memoryId = Objects.requireNonNull(memoryId, "memoryId must not be null");
    }

    public Long getMemoryId() {
        return memoryId;
    }

    public synchronized Optional<SkillActivation> findActiveSkill(String skillName) {
        return Optional.ofNullable(activeSkills.get(skillName));
    }

    public synchronized boolean isActive(String skillName) {
        return activeSkills.containsKey(skillName);
    }

    public synchronized List<String> getActiveSkillNames() {
        return new ArrayList<>(activeSkills.keySet());
    }

    public synchronized void activate(String skillName, int remainingTurns) {
        Objects.requireNonNull(skillName, "skillName must not be null");
        if (remainingTurns > 0) {
            activeSkills.put(skillName, new SkillActivation(skillName, remainingTurns));
        } else {
            activeSkills.remove(skillName);
        }
    }

    public synchronized void deactivate(String skillName) {
        activeSkills.remove(skillName);
    }

    public synchronized void consumeTurn(String skillName) {
        SkillActivation activation = activeSkills.get(skillName);
        if (activation == null) {
            return;
        }
        activation.decrementRemainingTurns();
        if (activation.getRemainingTurns() <= 0) {
            activeSkills.remove(skillName);
        }
    }

    public synchronized Map<String, SkillActivation> getActiveSkills() {
        return new LinkedHashMap<>(activeSkills);
    }
}