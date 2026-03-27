package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.service.impl.ConversationSummaryServiceImpl;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.spring.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Qwen35FlashWiringTest {

    @Test
    void shouldBindXiaozhiAgentToDedicatedQwen35FlashStreamingBean() {
        AiService aiService = XiaozhiAgent.class.getAnnotation(AiService.class);

        assertNotNull(aiService);
        assertEquals("qwen35FlashStreamingChatModel", aiService.streamingChatModel());
    }

    @Test
    void shouldInjectConversationSummaryServiceWithDedicatedQwen35FlashChatModel() throws NoSuchFieldException {
        Field chatModelField = ConversationSummaryServiceImpl.class.getDeclaredField("openAiChatModel");

        assertEquals(OpenAiChatModel.class, chatModelField.getType());
        Qualifier qualifier = chatModelField.getAnnotation(Qualifier.class);
        assertNotNull(qualifier);
        assertEquals("qwen35FlashChatModel", qualifier.value());
    }
}