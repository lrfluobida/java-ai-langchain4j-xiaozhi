package com.atguigu.java.ai.langchain4j.skill;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import com.atguigu.java.ai.langchain4j.controller.XiaozhiController;
import com.atguigu.java.ai.langchain4j.service.ConversationSummaryService;
import com.atguigu.java.ai.langchain4j.skill.service.SkillPromptService;
import com.atguigu.java.ai.langchain4j.skill.session.SkillSessionStore;
import com.atguigu.java.ai.langchain4j.store.MongoChatMemoryStore;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(XiaozhiController.class)
class XiaozhiSkillIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private XiaozhiAgent xiaozhiAgent;

    @MockBean
    private SkillPromptService skillPromptService;

    @MockBean
    private ConversationSummaryService conversationSummaryService;

    @Test
    void shouldResolveGenericSkillRulesBeforeCallingAgent() {
        when(conversationSummaryService.getSummary(3001L)).thenReturn("history summary");
        when(skillPromptService.resolveSkillRulesWithDisclosure(3001L, "我要预约挂号")).thenReturn("generic skill rules");
        when(xiaozhiAgent.chat(3001L, "我要预约挂号", "history summary", "generic skill rules"))
                .thenReturn(Flux.just("ok"));

        webTestClient.post()
                .uri("/xiaozhi/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chatForm(3001L, "我要预约挂号"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");

        verify(skillPromptService).resolveSkillRulesWithDisclosure(3001L, "我要预约挂号");
        verify(xiaozhiAgent).chat(3001L, "我要预约挂号", "history summary", "generic skill rules");
    }

    @Test
    void deleteMessagesShouldAlsoClearSkillSessionStore() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ConversationSummaryService conversationSummaryService = mock(ConversationSummaryService.class);
        SkillSessionStore skillSessionStore = mock(SkillSessionStore.class);

        MongoChatMemoryStore store = new MongoChatMemoryStore();
        ReflectionTestUtils.setField(store, "mongoTemplate", mongoTemplate);
        ReflectionTestUtils.setField(store, "conversationSummaryService", conversationSummaryService);
        ReflectionTestUtils.setField(store, "skillSessionStore", skillSessionStore);

        store.updateMessages(3001L, List.of(UserMessage.from("hello")));
        store.deleteMessages(3001L);

        verify(mongoTemplate).remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(com.atguigu.java.ai.langchain4j.bean.MyChatMessages.class));
        verify(conversationSummaryService).deleteSummary(3001L);
        verify(skillSessionStore).remove(3001L);
    }

    private ChatForm chatForm(Long memoryId, String message) {
        ChatForm chatForm = new ChatForm();
        chatForm.setMemoryId(memoryId);
        chatForm.setMessage(message);
        return chatForm;
    }
}
