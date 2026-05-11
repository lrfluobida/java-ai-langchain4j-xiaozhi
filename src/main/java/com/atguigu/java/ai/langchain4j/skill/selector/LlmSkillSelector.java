package com.atguigu.java.ai.langchain4j.skill.selector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmSkillSelector implements SkillSelector {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public LlmSkillSelector(@Qualifier("qwen35FlashChatModel") ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Override
    public SkillSelectionResult select(SkillSelectionContext context) {
        String response = chatModel.chat(buildPrompt(context));
        return parseResult(response);
    }

    private String buildPrompt(SkillSelectionContext context) {
        return """
                你是 Skill 路由器，只负责根据用户消息选择需要激活的 skills。
                只能从 availableSkills 中选择。
                不要回答用户问题。
                必须返回合法 JSON，不要输出 Markdown。
                如果不需要任何 skill，返回 selectedSkills: []。

                JSON 格式：
                {
                  "selectedSkills": ["skill-name"],
                  "deactivatedSkills": [],
                  "confidence": 0.0,
                  "reason": "简短原因"
                }

                用户消息：
                %s

                当前已激活 skills：
                %s

                可用 skills：
                %s
                """.formatted(
                nullToEmpty(context.userMessage()),
                toJson(context.activeSkillNames()),
                toJson(context.availableSkills())
        );
    }

    private SkillSelectionResult parseResult(String response) {
        String json = extractJson(response);
        try {
            return objectMapper.readValue(json, SkillSelectionResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to parse skill selection result: " + response, ex);
        }
    }

    private String extractJson(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Skill selection response is blank");
        }
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalStateException("Skill selection response does not contain JSON: " + response);
        }
        return trimmed.substring(start, end + 1);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : List.of());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize skill selection prompt data", ex);
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
