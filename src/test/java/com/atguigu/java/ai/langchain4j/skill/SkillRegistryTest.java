package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.loader.SkillLoader;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.registry.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRegistryTest {

    @Test
    void shouldReturnEnabledSkillsSortedByDescendingPriority() {
        SkillRegistry registry = new SkillRegistry(new StubSkillLoader(List.of(
                skill("low", true, 10),
                skill("disabled", false, 999),
                skill("high", true, 100)
        )));

        List<SkillDefinition> enabledSkills = registry.getEnabledSkills();

        assertEquals(2, enabledSkills.size());
        assertEquals("high", enabledSkills.get(0).getName());
        assertEquals("low", enabledSkills.get(1).getName());
        assertTrue(enabledSkills.stream().allMatch(skill -> skill.getRouteConfig().getEnabled()));
    }

    @Test
    void shouldFindSkillByName() {
        SkillRegistry registry = new SkillRegistry(new StubSkillLoader(List.of(
                skill("appointment", true, 100),
                skill("other", true, 5)
        )));

        Optional<SkillDefinition> result = registry.findByName("appointment");

        assertTrue(result.isPresent());
        assertEquals("appointment", result.get().getName());
        assertFalse(registry.findByName("missing").isPresent());
    }

    private static SkillDefinition skill(String name, boolean enabled, int priority) {
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName(name);
        routeConfig.setEnabled(enabled);
        routeConfig.setPriority(priority);
        return new SkillDefinition(name, name + " description", 1, "content for " + name, routeConfig);
    }

    private static class StubSkillLoader implements SkillLoader {

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