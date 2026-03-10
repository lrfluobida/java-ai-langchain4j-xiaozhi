package com.atguigu.java.ai.langchain4j.service;

import com.atguigu.java.ai.langchain4j.bean.KnowledgeFileInfo;
import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestResult;

import java.util.List;

public interface KnowledgeService {

    /**
     * 查看当前可导入的知识库文件
     * @return 知识库文件列表
     */
    List<KnowledgeFileInfo> listKnowledgeFiles();

    /**
     * 导入指定知识库文件
     * @param fileNames 文件名列表
     * @return 导入结果
     */
    KnowledgeIngestResult ingestFiles(List<String> fileNames);
}
