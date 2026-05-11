package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.loader.SkillLoader;
import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.registry.SkillRegistry;
import com.atguigu.java.ai.langchain4j.skill.router.SkillRouter;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionContext;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionResult;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionService;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelector;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillSelectionServiceTest {

    @Test
    void shouldSelectSkillFromLlmResultWithoutKeywordMatch() {
        SkillDefinition appointment = skill("appointment", true, 3, List.of("book"));
        SkillSelectionService service = service(List.of(appointment), context ->
                new SkillSelectionResult(List.of("appointment"), List.of(), 0.9, "appointment intent"));

        List<SkillDefinition> selected = service.select(new SkillContext(1L, "I need to see a doctor"));

        assertThat(selected).extracting(SkillDefinition::getName).containsExactly("appointment");
    }

    @Test
    void shouldFallbackToKeywordRouterWhenLlmSelectionFails() {
        SkillDefinition appointment = skill("appointment", true, 3, List.of("book"));
        SkillSelectionService service = service(List.of(appointment), context -> {
            throw new IllegalStateException("llm unavailable");
        });

        List<SkillDefinition> selected = service.select(new SkillContext(1L, "please book an appointment"));

        assertThat(selected).extracting(SkillDefinition::getName).containsExactly("appointment");
    }

    @Test
    void shouldKeepStickySkillActiveWhenLlmDoesNotDeactivateIt() {
        SkillDefinition appointment = skill("appointment", true, 2, List.of("book"));
        SkillSelectionService service = service(List.of(appointment), new SkillSelector() {
            private int callCount;

            @Override
            public SkillSelectionResult select(SkillSelectionContext context) {
                callCount++;
                if (callCount == 1) {
                    return new SkillSelectionResult(List.of("appointment"), List.of(), 0.9, "appointment intent");
                }
                return new SkillSelectionResult(List.of(), List.of(), 0.6, "follow up");
            }
        });

        List<SkillDefinition> firstTurn = service.select(new SkillContext(1L, "I need an appointment"));
        List<SkillDefinition> secondTurn = service.select(new SkillContext(1L, "what time slots are available"));

        assertThat(firstTurn).extracting(SkillDefinition::getName).containsExactly("appointment");
        assertThat(secondTurn).extracting(SkillDefinition::getName).containsExactly("appointment");
    }

    private static SkillSelectionService service(List<SkillDefinition> skills, SkillSelector selector) {
        SkillSessionStore sessionStore = new SkillSessionStore();
        SkillRegistry registry = new SkillRegistry(new StubSkillLoader(skills));
        return new SkillSelectionService(registry, sessionStore, selector, new SkillRouter(registry, sessionStore));
    }

    private static SkillDefinition skill(String name, boolean stickySession, int maxActiveTurns, List<String> entryKeywords) {
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName(name);
        routeConfig.setEnabled(true);
        routeConfig.setPriority(100);
        routeConfig.setStickySession(stickySession);
        routeConfig.setMaxActiveTurns(maxActiveTurns);
        routeConfig.setEntryKeywords(entryKeywords);
        return new SkillDefinition(name, name + " description", 1, "content for " + name, routeConfig);
    }

    private static final class StubSkillLoader implements SkillLoader {

        private final List<SkillDefinition> skills;

        private StubSkillLoader(List<SkillDefinition> skills) {
            this.skills = skills;
        }

        @Override
        public List<SkillDefinition> loadSkills() {
            return skills;
        }
    }
}
