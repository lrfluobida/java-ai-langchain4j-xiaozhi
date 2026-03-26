package com.atguigu.java.ai.langchain4j.skill.model;

import java.util.ArrayList;
import java.util.List;

public class SkillRouteConfig {

    private String name;
    private Boolean enabled = Boolean.TRUE;
    private Integer priority = 0;
    private List<String> entryKeywords = new ArrayList<>();
    private Boolean stickySession = Boolean.FALSE;
    private List<String> exitKeywords = new ArrayList<>();
    private Integer maxActiveTurns = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public List<String> getEntryKeywords() {
        return entryKeywords;
    }

    public void setEntryKeywords(List<String> entryKeywords) {
        this.entryKeywords = entryKeywords;
    }

    public Boolean getStickySession() {
        return stickySession;
    }

    public void setStickySession(Boolean stickySession) {
        this.stickySession = stickySession;
    }

    public List<String> getExitKeywords() {
        return exitKeywords;
    }

    public void setExitKeywords(List<String> exitKeywords) {
        this.exitKeywords = exitKeywords;
    }

    public Integer getMaxActiveTurns() {
        return maxActiveTurns;
    }

    public void setMaxActiveTurns(Integer maxActiveTurns) {
        this.maxActiveTurns = maxActiveTurns;
    }
}