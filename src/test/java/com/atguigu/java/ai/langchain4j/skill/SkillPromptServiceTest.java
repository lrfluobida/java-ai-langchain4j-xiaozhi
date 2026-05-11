package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.prompt.SkillPromptAssembler;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionService;
import com.atguigu.java.ai.langchain4j.skill.service.SkillPromptService;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillPromptServiceTest {

    @Test
    void shouldReturnAppointmentPromptForAppointmentIntent() {
        SkillSelectionService skillSelectionService = Mockito.mock(SkillSelectionService.class);
        when(skillSelectionService.select(any(SkillContext.class))).thenAnswer(invocation -> {
            SkillContext context = invocation.getArgument(0);
            assertEquals(3001L, context.memoryId());
            assertEquals("I want to book an appointment", context.message());
            return List.of(skill("appointment", "appointment prompt"));
        });

        SkillPromptService skillPromptService = new SkillPromptService(
                skillSelectionService,
                new SkillPromptAssembler(),
                new SkillSessionStore()
        );

        assertEquals("appointment prompt", skillPromptService.resolveSkillRules(3001L, "I want to book an appointment"));
        verify(skillSelectionService).select(any(SkillContext.class));
    }

    @Test
    void shouldReturnEmptyStringWhenRouterReturnsNoSkills() {
        SkillSelectionService skillSelectionService = Mockito.mock(SkillSelectionService.class);
        when(skillSelectionService.select(any(SkillContext.class))).thenReturn(List.of());

        SkillPromptService skillPromptService = new SkillPromptService(
                skillSelectionService,
                new SkillPromptAssembler(),
                new SkillSessionStore()
        );

        assertEquals("", skillPromptService.resolveSkillRules(3001L, "I want to book an appointment"));
    }

    private SkillDefinition skill(String name, String content) {
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName(name);
        return new SkillDefinition(name, null, 1, content, routeConfig);
    }
}
