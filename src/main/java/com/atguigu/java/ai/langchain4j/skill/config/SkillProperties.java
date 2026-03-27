package com.atguigu.java.ai.langchain4j.skill.config;

import com.atguigu.java.ai.langchain4j.skill.model.SkillRouteConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties
@PropertySource(value = "classpath:skills.yml", factory = YamlPropertySourceFactory.class)
public class SkillProperties {

    private List<SkillRouteConfig> skills = new ArrayList<>();

    public List<SkillRouteConfig> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillRouteConfig> skills) {
        this.skills = skills;
    }
}