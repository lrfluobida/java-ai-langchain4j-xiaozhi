package com.atguigu.java.ai.langchain4j.skill.loader;

import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;

import java.util.List;

public interface SkillLoader {

    List<SkillDefinition> loadSkills();
}