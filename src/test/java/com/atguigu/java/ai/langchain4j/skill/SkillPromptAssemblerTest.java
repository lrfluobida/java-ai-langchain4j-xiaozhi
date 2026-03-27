package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import com.atguigu.java.ai.langchain4j.skill.prompt.SkillPromptAssembler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillPromptAssemblerTest {

    @Test
    void shouldReturnEmptyStringWhenNoSkillsMatch() {
        SkillPromptAssembler assembler = new SkillPromptAssembler();

        assertEquals("", assembler.assemble(List.of()));
    }

    @Test
    void shouldPreserveSkillOrderWhenAssemblingPrompt() {
        SkillPromptAssembler assembler = new SkillPromptAssembler();

        assertEquals("first\n\nsecond", assembler.assemble(List.of(
                skill("first"),
                skill("second")
        )));
    }

    private SkillDefinition skill(String content) {
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName(content);
        return new SkillDefinition(content, null, 1, content, routeConfig);
    }
}