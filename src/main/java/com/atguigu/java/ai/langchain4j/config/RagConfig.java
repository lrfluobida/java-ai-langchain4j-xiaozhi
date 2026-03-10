package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.model.embedding.onnx.HuggingFaceTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Autowired
    private RagProperties ragProperties;

    @Bean
    public DocumentSplitter ragDocumentSplitter() {
        // 自定义文档分段器
        return new DocumentByParagraphSplitter(
                ragProperties.getMaxSegmentSize(),
                ragProperties.getMaxOverlapSize(),
                new HuggingFaceTokenizer()
        );
    }

    @Bean
    public TextDocumentParser textDocumentParser() {
        // 文本文件解析器
        return new TextDocumentParser();
    }

    @Bean
    public ApachePdfBoxDocumentParser apachePdfBoxDocumentParser() {
        // PDF 文件解析器
        return new ApachePdfBoxDocumentParser();
    }
}
