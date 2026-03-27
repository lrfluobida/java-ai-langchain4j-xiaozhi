package com.atguigu.java.ai.langchain4j.skill.service;

import com.atguigu.java.ai.langchain4j.skill.model.SkillContext;
import com.atguigu.java.ai.langchain4j.skill.model.SkillDefinition;
import com.atguigu.java.ai.langchain4j.skill.prompt.SkillPromptAssembler;
import com.atguigu.java.ai.langchain4j.skill.router.SkillRouter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillPromptService {

    private final SkillRouter skillRouter;
    private final SkillPromptAssembler skillPromptAssembler;

    public SkillPromptService(SkillRouter skillRouter, SkillPromptAssembler skillPromptAssembler) {
        this.skillRouter = skillRouter;
        this.skillPromptAssembler = skillPromptAssembler;
    }

    public String resolveSkillRules(Long memoryId, String userMessage) {
        List<SkillDefinition> matchedSkills = skillRouter.route(new SkillContext(memoryId, userMessage));
        return skillPromptAssembler.assemble(matchedSkills);
    }
}