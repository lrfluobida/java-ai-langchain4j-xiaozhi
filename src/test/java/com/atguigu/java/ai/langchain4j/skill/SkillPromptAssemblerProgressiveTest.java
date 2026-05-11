package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.model.DisclosureLevel;
import com.atguigu.java.ai.langchain4j.skill.model.SkillActivation;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.prompt.SkillPromptAssembler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillPromptAssemblerProgressiveTest {

    private final SkillPromptAssembler assembler = new SkillPromptAssembler();

    @Test
    void shouldAssembleBasicLevelForNewActivation() {
        SkillDefinition skill = createProgressiveSkill();
        SkillActivation activation = new SkillActivation("test", 10);
        activation.setCurrentDisclosureLevel(DisclosureLevel.BASIC);

        String result = assembler.assembleWithDisclosure(
                List.of(skill),
                Map.of("test", activation)
        );

        assertThat(result)
                .contains("Basic instruction")
                .doesNotContain("Standard instruction")
                .doesNotContain("Advanced instruction");
    }

    @Test
    void shouldAssembleStandardLevelAfterUpgrade() {
        SkillDefinition skill = createProgressiveSkill();
        SkillActivation activation = new SkillActivation("test", 10);
        activation.setCurrentDisclosureLevel(DisclosureLevel.STANDARD);

        String result = assembler.assembleWithDisclosure(
                List.of(skill),
                Map.of("test", activation)
        );

        assertThat(result)
                .contains("Basic instruction")
                .contains("Standard instruction")
                .doesNotContain("Advanced instruction");
    }

    @Test
    void shouldAssembleAdvancedLevelForComplexScenario() {
        SkillDefinition skill = createProgressiveSkill();
        SkillActivation activation = new SkillActivation("test", 10);
        activation.setCurrentDisclosureLevel(DisclosureLevel.ADVANCED);

        String result = assembler.assembleWithDisclosure(
                List.of(skill),
                Map.of("test", activation)
        );

        assertThat(result)
                .contains("Basic instruction")
                .contains("Standard instruction")
                .contains("Advanced instruction");
    }

    @Test
    void shouldHandleNonProgressiveSkill() {
        SkillDefinition skill = createNonProgressiveSkill();

        String result = assembler.assembleWithDisclosure(
                List.of(skill),
                Map.of()
        );

        assertThat(result).contains("All content at once");
    }

    @Test
    void shouldHandleMixedSkills() {
        SkillDefinition progressive = createProgressiveSkill();
        SkillDefinition nonProgressive = createNonProgressiveSkill();

        SkillActivation activation = new SkillActivation("progressive", 10);
        activation.setCurrentDisclosureLevel(DisclosureLevel.BASIC);

        String result = assembler.assembleWithDisclosure(
                List.of(progressive, nonProgressive),
                Map.of("progressive", activation)
        );

        assertThat(result)
                .contains("Basic instruction")
                .contains("All content at once")
                .doesNotContain("Standard instruction");
    }

    private SkillDefinition createProgressiveSkill() {
        SkillDefinition skill = mock(SkillDefinition.class);
        when(skill.getName()).thenReturn("test");
        when(skill.isProgressive()).thenReturn(true);
        when(skill.getContentForLevel(DisclosureLevel.BASIC))
                .thenReturn("Basic instruction 1\nBasic instruction 2");
        when(skill.getContentForLevel(DisclosureLevel.STANDARD))
                .thenReturn("Basic instruction 1\nBasic instruction 2\n\nStandard instruction 1");
        when(skill.getContentForLevel(DisclosureLevel.ADVANCED))
                .thenReturn("Basic instruction 1\nBasic instruction 2\n\nStandard instruction 1\n\nAdvanced instruction 1");
        return skill;
    }

    private SkillDefinition createNonProgressiveSkill() {
        SkillDefinition skill = mock(SkillDefinition.class);
        when(skill.getName()).thenReturn("non-progressive");
        when(skill.isProgressive()).thenReturn(false);
        when(skill.getContent()).thenReturn("All content at once");
        return skill;
    }
}
