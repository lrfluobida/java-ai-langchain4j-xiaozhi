package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.config.SkillProperties;
import com.atguigu.java.ai.langchain4j.skill.loader.ClasspathSkillLoader;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathSkillLoaderTest {

    @Test
    void shouldMarkSpringInjectionConstructorExplicitly() throws NoSuchMethodException {
        Constructor<ClasspathSkillLoader> constructor = ClasspathSkillLoader.class.getConstructor(SkillProperties.class);

        assertTrue(constructor.isAnnotationPresent(Autowired.class));
    }


    @Test
    void shouldLoadSkillMarkdownAndMergeMatchingRouteConfig() {
        SkillProperties skillProperties = new SkillProperties();
        SkillRouteConfig routeConfig = new SkillRouteConfig();
        routeConfig.setName("appointment");
        routeConfig.setEnabled(true);
        routeConfig.setPriority(100);
        routeConfig.setStickySession(true);
        routeConfig.setMaxActiveTurns(12);
        SkillRouteConfig progressiveRouteConfig = new SkillRouteConfig();
        progressiveRouteConfig.setName("appointment-progressive");
        progressiveRouteConfig.setEnabled(true);
        progressiveRouteConfig.setPriority(100);
        progressiveRouteConfig.setStickySession(true);
        progressiveRouteConfig.setMaxActiveTurns(12);
        skillProperties.setSkills(List.of(routeConfig, progressiveRouteConfig));

        ClasspathSkillLoader loader = new ClasspathSkillLoader(skillProperties);

        List<SkillDefinition> skills = loader.loadSkills();

        assertEquals(2, skills.size());

        SkillDefinition skill = skills.stream()
                .filter(candidate -> "appointment".equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("appointment", skill.getName());
        assertEquals("处理预约挂号、取消预约和号源查询的技能", skill.getDescription());
        assertEquals(1, skill.getVersion());
        assertTrue(skill.getContent().startsWith("# Appointment Skill"));
        assertTrue(skill.getContent().contains("当用户想要挂号"));
        assertEquals(routeConfig, skill.getRouteConfig());
    }

    @Test
    void shouldFailFastWhenRouteConfigIsMissing() {
        SkillProperties skillProperties = new SkillProperties();
        skillProperties.setSkills(List.of());

        ClasspathSkillLoader loader = new ClasspathSkillLoader(skillProperties);

        IllegalStateException exception = assertThrows(IllegalStateException.class, loader::loadSkills);

        assertTrue(exception.getMessage().contains("appointment"));
    }
}
