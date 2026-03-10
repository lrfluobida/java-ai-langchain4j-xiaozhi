package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIServiceTest {

    @Autowired
    private Assistant assistant;
    @Test
    public void testAssistant() {
        String ans = assistant.chat("你是谁？");
        System.out.println(ans);

    }
}
