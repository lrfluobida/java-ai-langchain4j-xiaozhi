package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Qwen35FlashModelConfig {

    @Bean("qwen35FlashChatModel")
    public OpenAiChatModel qwen35FlashChatModel(
            @Value("${xiaozhi.llm.qwen35-flash.base-url}") String baseUrl,
            @Value("${xiaozhi.llm.qwen35-flash.api-key}") String apiKey,
            @Value("${xiaozhi.llm.qwen35-flash.model-name}") String modelName,
            @Value("${xiaozhi.llm.qwen35-flash.log-requests:false}") boolean logRequests,
            @Value("${xiaozhi.llm.qwen35-flash.log-responses:false}") boolean logResponses
    ) {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }

    @Bean("qwen35FlashStreamingChatModel")
    public OpenAiStreamingChatModel qwen35FlashStreamingChatModel(
            @Value("${xiaozhi.llm.qwen35-flash.base-url}") String baseUrl,
            @Value("${xiaozhi.llm.qwen35-flash.api-key}") String apiKey,
            @Value("${xiaozhi.llm.qwen35-flash.model-name}") String modelName,
            @Value("${xiaozhi.llm.qwen35-flash.log-requests:false}") boolean logRequests,
            @Value("${xiaozhi.llm.qwen35-flash.log-responses:false}") boolean logResponses
    ) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}