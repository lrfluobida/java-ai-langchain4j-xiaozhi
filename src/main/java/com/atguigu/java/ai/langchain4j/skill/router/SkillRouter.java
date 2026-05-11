package com.atguigu.java.ai.langchain4j.skill.router;

import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.model.SkillSessionState;
import com.atguigu.java.ai.langchain4j.skill.registry.SkillRegistry;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class SkillRouter {

    private final SkillRegistry skillRegistry;
    private final SkillSessionStore skillSessionStore;

    public SkillRouter(SkillRegistry skillRegistry, SkillSessionStore skillSessionStore) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry must not be null");
        this.skillSessionStore = Objects.requireNonNull(skillSessionStore, "skillSessionStore must not be null");
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public List<SkillDefinition> route(SkillContext context) {
        Objects.requireNonNull(context, "context must not be null");
        SkillSessionState sessionState = skillSessionStore.getOrCreate(context.memoryId());
        String message = context.message();

        for (String activeSkillName : sessionState.getActiveSkillNames()) {
            skillRegistry.findByName(activeSkillName).ifPresentOrElse(skill -> {
                if (containsAny(message, skill.getRouteConfig().getExitKeywords())) {
                    sessionState.deactivate(activeSkillName);
                }
            }, () -> sessionState.deactivate(activeSkillName));
        }

        List<SkillDefinition> routedSkills = new ArrayList<>();
        for (SkillDefinition skill : skillRegistry.getEnabledSkills()) {
            SkillRouteConfig routeConfig = skill.getRouteConfig();
            String skillName = skill.getName();
            boolean entryMatched = containsAny(message, routeConfig.getEntryKeywords());

            if (entryMatched) {
                routedSkills.add(skill);
                if (Boolean.TRUE.equals(routeConfig.getStickySession())) {
                    sessionState.activate(skillName, futureTurns(routeConfig));
                } else {
                    sessionState.deactivate(skillName);
                }
                continue;
            }

            if (Boolean.TRUE.equals(routeConfig.getStickySession())) {
                SkillActivation activation = sessionState.findActiveSkill(skillName).orElse(null);
                if (activation != null && activation.getRemainingTurns() > 0) {
                    routedSkills.add(skill);
                    sessionState.consumeTurn(skillName);
                } else {
                    sessionState.deactivate(skillName);
                }
            } else {
                sessionState.deactivate(skillName);
            }
        }
        return routedSkills;
    }

    private int futureTurns(SkillRouteConfig routeConfig) {
        return Math.max(0, routeConfig.getMaxActiveTurns() - 1);
    }

    private boolean containsAny(String message, List<String> keywords) {
        if (message == null || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
