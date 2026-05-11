package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.config.SkillProperties;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SkillProperties.class)
@EnableConfigurationProperties(SkillProperties.class)
class SkillPropertiesTest {

    @Autowired
    private SkillProperties skillProperties;

    @Test
    void shouldBindAppointmentSkillConfig() {
        SkillRouteConfig config = skillProperties.getSkills().get(0);

        assertEquals("appointment", config.getName());
        assertEquals(false, config.getEnabled());
        assertEquals(100, config.getPriority());
        assertEquals(9, config.getEntryKeywords().size());
        assertTrue(config.getEntryKeywords().contains("挂号"));
        assertTrue(config.getStickySession());
        assertTrue(config.getExitKeywords().isEmpty());
        assertEquals(12, config.getMaxActiveTurns());

        SkillRouteConfig progressiveConfig = skillProperties.getSkills().get(1);
        assertEquals("appointment-progressive", progressiveConfig.getName());
        assertTrue(progressiveConfig.getEnabled());
    }
}
