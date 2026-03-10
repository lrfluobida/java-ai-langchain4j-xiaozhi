package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.KnowledgeFileInfo;
import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestResult;
import com.atguigu.java.ai.langchain4j.service.KnowledgeService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

@SpringBootTest
public class RAGTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Test
    public void testListKnowledgeFiles() {
        List<KnowledgeFileInfo> knowledgeFiles = knowledgeService.listKnowledgeFiles();
        knowledgeFiles.forEach(System.out::println);
    }

    @Disabled("Manual test for knowledge ingestion")
    @Test
    public void testIngestKnowledgeFiles() {
        KnowledgeIngestResult ingestResult = knowledgeService.ingestFiles(Collections.singletonList("人工智能.md"));
        System.out.println(ingestResult);
    }
}
