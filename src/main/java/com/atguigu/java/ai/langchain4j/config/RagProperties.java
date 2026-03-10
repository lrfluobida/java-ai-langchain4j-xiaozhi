package com.atguigu.java.ai.langchain4j.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "xiaozhi.rag")
public class RagProperties {

    // 知识库目录
    private String knowledgeDir = "knowledge";
    // 允许导入的文件类型
    private List<String> allowedExtensions = new ArrayList<>(List.of("md", "txt", "pdf"));
    // 文档分段最大长度
    private Integer maxSegmentSize = 300;
    // 文档分段重叠长度
    private Integer maxOverlapSize = 30;

    public String getKnowledgeDir() {
        return knowledgeDir;
    }

    public void setKnowledgeDir(String knowledgeDir) {
        this.knowledgeDir = knowledgeDir;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Integer getMaxSegmentSize() {
        return maxSegmentSize;
    }

    public void setMaxSegmentSize(Integer maxSegmentSize) {
        this.maxSegmentSize = maxSegmentSize;
    }

    public Integer getMaxOverlapSize() {
        return maxOverlapSize;
    }

    public void setMaxOverlapSize(Integer maxOverlapSize) {
        this.maxOverlapSize = maxOverlapSize;
    }
}
