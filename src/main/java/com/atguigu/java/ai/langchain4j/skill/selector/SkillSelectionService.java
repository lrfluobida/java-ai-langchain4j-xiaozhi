package com.atguigu.java.ai.langchain4j.skill.selector;

import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.model.SkillSessionState;
import com.atguigu.java.ai.langchain4j.skill.registry.SkillRegistry;
import com.atguigu.java.ai.langchain4j.skill.router.SkillRouter;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class SkillSelectionService {

    private static final Logger log = LoggerFactory.getLogger(SkillSelectionService.class);

    private final SkillRegistry skillRegistry;
    private final SkillSessionStore skillSessionStore;
    private final SkillSelector skillSelector;
    private final SkillRouter fallbackRouter;

    public SkillSelectionService(SkillRegistry skillRegistry,
                                 SkillSessionStore skillSessionStore,
                                 SkillSelector skillSelector,
                                 SkillRouter fallbackRouter) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry must not be null");
        this.skillSessionStore = Objects.requireNonNull(skillSessionStore, "skillSessionStore must not be null");
        this.skillSelector = Objects.requireNonNull(skillSelector, "skillSelector must not be null");
        this.fallbackRouter = Objects.requireNonNull(fallbackRouter, "fallbackRouter must not be null");
    }

    public List<SkillDefinition> select(SkillContext context) {
        Objects.requireNonNull(context, "context must not be null");
        SkillSessionState sessionState = skillSessionStore.getOrCreate(context.memoryId());
        List<SkillDefinition> enabledSkills = skillRegistry.getEnabledSkills();
        List<String> activeSkillNames = sessionState.getActiveSkillNames();
        SkillSelectionContext selectionContext = new SkillSelectionContext(
                context.memoryId(),
                context.message(),
                activeSkillNames,
                enabledSkills.stream().map(SkillMetadata::from).toList()
        );

        SkillSelectionResult result;
        try {
            result = skillSelector.select(selectionContext);
        } catch (RuntimeException ex) {
            List<SkillDefinition> fallbackSkills = fallbackRouter.route(context);
            log.debug("Skill selection fallback used. memoryId={}, availableSkills={}, activeSkills={}, reason={}, finalSkills={}",
                    context.memoryId(),
                    enabledSkills.stream().map(SkillDefinition::getName).toList(),
                    activeSkillNames,
                    ex.getMessage(),
                    fallbackSkills.stream().map(SkillDefinition::getName).toList());
            return fallbackSkills;
        }

        List<SkillDefinition> selectedSkills = applySelection(result, sessionState);
        log.debug("Skill selection completed. memoryId={}, availableSkills={}, activeSkills={}, llmSelectedSkills={}, llmDeactivatedSkills={}, confidence={}, finalSkills={}",
                context.memoryId(),
                enabledSkills.stream().map(SkillDefinition::getName).toList(),
                activeSkillNames,
                safeResult(result).selectedSkills(),
                safeResult(result).deactivatedSkills(),
                safeResult(result).confidence(),
                selectedSkills.stream().map(SkillDefinition::getName).toList());
        return selectedSkills;
    }

    private List<SkillDefinition> applySelection(SkillSelectionResult result, SkillSessionState sessionState) {
        Set<String> selectedNames = new LinkedHashSet<>(safeResult(result).selectedSkills());
        Set<String> deactivatedNames = new LinkedHashSet<>(safeResult(result).deactivatedSkills());
        deactivatedNames.forEach(sessionState::deactivate);

        LinkedHashSet<String> finalNames = new LinkedHashSet<>();
        for (SkillDefinition skill : skillRegistry.getEnabledSkills()) {
            String skillName = skill.getName();
            SkillRouteConfig routeConfig = skill.getRouteConfig();

            if (selectedNames.contains(skillName) && !deactivatedNames.contains(skillName)) {
                finalNames.add(skillName);
                if (Boolean.TRUE.equals(routeConfig.getStickySession())) {
                    sessionState.activate(skillName, futureTurns(routeConfig));
                } else {
                    sessionState.deactivate(skillName);
                }
                continue;
            }

            if (Boolean.TRUE.equals(routeConfig.getStickySession())) {
                SkillActivation activation = sessionState.findActiveSkill(skillName).orElse(null);
                if (activation != null && activation.getRemainingTurns() > 0 && !deactivatedNames.contains(skillName)) {
                    finalNames.add(skillName);
                    sessionState.consumeTurn(skillName);
                } else if (!selectedNames.contains(skillName)) {
                    sessionState.deactivate(skillName);
                }
            } else if (!selectedNames.contains(skillName)) {
                sessionState.deactivate(skillName);
            }
        }

        return skillRegistry.getEnabledSkills().stream()
                .filter(skill -> finalNames.contains(skill.getName()))
                .toList();
    }

    private SkillSelectionResult safeResult(SkillSelectionResult result) {
        return result != null ? result : new SkillSelectionResult(List.of(), List.of(), 0.0, "empty result");
    }

    private int futureTurns(SkillRouteConfig routeConfig) {
        return Math.max(0, routeConfig.getMaxActiveTurns() - 1);
    }
}
