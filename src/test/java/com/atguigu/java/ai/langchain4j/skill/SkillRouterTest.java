package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.loader.SkillLoader;
import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.registry.SkillRegistry;
import com.atguigu.java.ai.langchain4j.skill.router.SkillRouter;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRouterTest {

    @Test
    void shouldActivateAppointmentSkillFromEntryKeywords() {
        SkillRouter router = router(List.of(skill("appointment", 100, true, 3, List.of("book"), List.of())));

        List<SkillDefinition> routedSkills = router.route(new SkillContext("memory-1", "please book an appointment"));

        assertEquals(1, routedSkills.size());
        assertEquals("appointment", routedSkills.get(0).getName());
    }

    @Test
    void shouldKeepStickySessionActiveAcrossTurns() {
        SkillRouter router = router(List.of(skill("appointment", 100, true, 2, List.of("book"), List.of())));

        List<SkillDefinition> firstTurn = router.route(new SkillContext("memory-1", "please book an appointment"));
        List<SkillDefinition> secondTurn = router.route(new SkillContext("memory-1", "follow up question"));

        assertEquals(1, firstTurn.size());
        assertEquals(1, secondTurn.size());
        assertEquals("appointment", secondTurn.get(0).getName());
    }

    @Test
    void shouldExpireSkillAfterMaxActiveTurns() {
        SkillRouter router = router(List.of(skill("appointment", 100, true, 2, List.of("book"), List.of())));

        List<SkillDefinition> firstTurn = router.route(new SkillContext("memory-1", "please book an appointment"));
        List<SkillDefinition> secondTurn = router.route(new SkillContext("memory-1", "follow up question"));
        List<SkillDefinition> thirdTurn = router.route(new SkillContext("memory-1", "another question"));

        assertEquals(1, firstTurn.size());
        assertEquals(1, secondTurn.size());
        assertTrue(thirdTurn.isEmpty());
    }

    @Test
    void shouldDeactivateActiveSkillWhenExitKeywordAppears() {
        SkillRouter router = router(List.of(skill("appointment", 100, true, 5, List.of("book"), List.of("stop"))));

        List<SkillDefinition> firstTurn = router.route(new SkillContext("memory-1", "please book an appointment"));
        List<SkillDefinition> secondTurn = router.route(new SkillContext("memory-1", "stop now"));

        assertEquals(1, firstTurn.size());
        assertTrue(secondTurn.isEmpty());
    }

    private static SkillRouter router(List<SkillDefinition> skills) {
        return new SkillRouter(new SkillRegistry(new StubSkillLoader(skills)), new SkillSessionStore());
    }

    private static SkillDefinition skill(String name,
                                         int priority,
                                         boolean stickySession,
                                         int maxActiveTurns,
                                         List<String> entryKeywords,
                                         List<String> exitKeywords) {
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName(name);
        routeConfig.setEnabled(true);
        routeConfig.setPriority(priority);
        routeConfig.setStickySession(stickySession);
        routeConfig.setMaxActiveTurns(maxActiveTurns);
        routeConfig.setEntryKeywords(entryKeywords);
        routeConfig.setExitKeywords(exitKeywords);
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