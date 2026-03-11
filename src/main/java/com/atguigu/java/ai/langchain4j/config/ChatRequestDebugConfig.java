package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnProperty(prefix = "xiaozhi.debug", name = "chat-request", havingValue = "true")
public class ChatRequestDebugConfig {

    @Bean
    public ChatModelListener finalChatRequestLoggingListener() {
        return new FinalChatRequestLoggingListener();
    }

    private static class FinalChatRequestLoggingListener implements ChatModelListener {

        private static final Logger log = LoggerFactory.getLogger(FinalChatRequestLoggingListener.class);

        @Override
        public void onRequest(ChatModelRequestContext requestContext) {
            ChatRequest chatRequest = requestContext.chatRequest();
            List<ChatMessage> messages = chatRequest.messages();
            if (messages == null || messages.isEmpty()) {
                return;
            }

            log.info("Final ChatRequest model={}, provider={}", chatRequest.modelName(), requestContext.modelProvider());
            log.info("Final ChatRequest.messages={}", ChatMessageSerializer.messagesToJson(messages));

            Optional<SystemMessage> systemMessage = SystemMessage.findFirst(messages);
            systemMessage.ifPresent(message -> log.info("Final SystemMessage.text=\n{}", message.text()));
        }
    }
}