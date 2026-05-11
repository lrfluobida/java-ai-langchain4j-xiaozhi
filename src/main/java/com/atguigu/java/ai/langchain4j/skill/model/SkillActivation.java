package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.Objects;

public class SkillActivation {

    private final String skillName;
    private int remainingTurns;
    private int activeTurns;
    private int toolCallCount;
    private boolean hasError;
    private DisclosureLevel currentDisclosureLevel;

    public SkillActivation(String skillName, int remainingTurns) {
        this.skillName = Objects.requireNonNull(skillName, "skillName must not be null");
        this.remainingTurns = Math.max(0, remainingTurns);
        this.activeTurns = 0;
        this.toolCallCount = 0;
        this.hasError = false;
        this.currentDisclosureLevel = DisclosureLevel.BASIC;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getRemainingTurns() {
        return remainingTurns;
    }

    public void setRemainingTurns(int remainingTurns) {
        this.remainingTurns = Math.max(0, remainingTurns);
    }

    public void decrementRemainingTurns() {
        if (remainingTurns > 0) {
            remainingTurns--;
        }
        activeTurns++;
    }

    public int getActiveTurns() {
        return activeTurns;
    }

    public int getToolCallCount() {
        return toolCallCount;
    }

    public void incrementToolCallCount() {
        this.toolCallCount++;
    }

    public boolean hasError() {
        return hasError;
    }

    public void markError() {
        this.hasError = true;
    }

    public DisclosureLevel getCurrentDisclosureLevel() {
        return currentDisclosureLevel;
    }

    public void setCurrentDisclosureLevel(DisclosureLevel level) {
        this.currentDisclosureLevel = level;
    }
}