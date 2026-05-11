package com.atguigu.java.ai.langchain4j.skill.selector;

public interface SkillSelector {

    SkillSelectionResult select(SkillSelectionContext context);
}
