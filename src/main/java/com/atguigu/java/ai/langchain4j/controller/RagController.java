package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.bean.KnowledgeFileInfo;
import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestForm;
import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestResult;
import com.atguigu.java.ai.langchain4j.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/rag")
public class RagController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Operation(summary = "查看可导入知识库文件")
    @GetMapping("/files")
    public List<KnowledgeFileInfo> listKnowledgeFiles() {
        return knowledgeService.listKnowledgeFiles();
    }

    @Operation(summary = "导入指定知识库文件")
    @PostMapping("/ingest")
    public KnowledgeIngestResult ingestFiles(@RequestBody(required = false) KnowledgeIngestForm knowledgeIngestForm) {
        // 请求体为空时返回空列表，避免空指针
        List<String> fileNames = knowledgeIngestForm == null ? Collections.emptyList() : knowledgeIngestForm.getFileNames();
        return knowledgeService.ingestFiles(fileNames);
    }
}
