package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.Objects;

public class SkillActivation {

    private final String skillName;
    private int remainingTurns;

    public SkillActivation(String skillName, int remainingTurns) {
        this.skillName = Objects.requireNonNull(skillName, "skillName must not be null");
        this.remainingTurns = Math.max(0, remainingTurns);
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
    }
}