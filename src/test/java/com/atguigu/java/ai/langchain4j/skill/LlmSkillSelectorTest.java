package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.skill.selector.LlmSkillSelector;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillMetadata;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionContext;
import com.atguigu.java.ai.langchain4j.skill.selector.SkillSelectionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmSkillSelectorTest {

    @Test
    void shouldParseJsonSelectionFromLlmResponse() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.chat(contains("可用 skills")))
                .thenReturn("""
                        {
                          "selectedSkills": ["appointment-progressive"],
                          "deactivatedSkills": [],
                          "confidence": 0.91,
                          "reason": "用户需要预约挂号"
                        }
                        """);
        LlmSkillSelector selector = new LlmSkillSelector(chatModel, new ObjectMapper());

        SkillSelectionResult result = selector.select(new SkillSelectionContext(
                1L,
                "我要预约挂号",
                List.of(),
                List.of(new SkillMetadata("appointment-progressive", "预约挂号", 1, 100, true, 12, true))
        ));

        assertThat(result.selectedSkills()).containsExactly("appointment-progressive");
        assertThat(result.deactivatedSkills()).isEmpty();
        assertThat(result.confidence()).isEqualTo(0.91);
        assertThat(result.reason()).isEqualTo("用户需要预约挂号");
    }
}
