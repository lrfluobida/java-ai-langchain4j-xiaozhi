package com.atguigu.java.ai.langchain4j.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Autowired
    private RagProperties ragProperties;

    @Bean
    public DocumentSplitter ragDocumentSplitter() {
        return new DocumentByParagraphSplitter(
                ragProperties.getMaxSegmentSize(),
                ragProperties.getMaxOverlapSize()
        );
    }

    @Bean
    public TextDocumentParser textDocumentParser() {
        return new TextDocumentParser();
    }

    @Bean
    public ApachePdfBoxDocumentParser apachePdfBoxDocumentParser() {
        return new ApachePdfBoxDocumentParser();
    }
}