package com.atguigu.java.ai.langchain4j.service.impl;

import com.atguigu.java.ai.langchain4j.bean.KnowledgeFileInfo;
import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestResult;
import com.atguigu.java.ai.langchain4j.config.RagProperties;
import com.atguigu.java.ai.langchain4j.service.KnowledgeService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RagProperties ragProperties;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private DocumentSplitter ragDocumentSplitter;

    @Autowired
    private TextDocumentParser textDocumentParser;

    @Autowired
    private ApachePdfBoxDocumentParser apachePdfBoxDocumentParser;

    @Override
    public List<KnowledgeFileInfo> listKnowledgeFiles() {
        Path baseDir = resolveKnowledgeDir();
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            return new ArrayList<>();
        }

        try (Stream<Path> pathStream = Files.walk(baseDir)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .sorted(Comparator.comparing(path -> toRelativeFileName(baseDir, path)))
                    .map(path -> toKnowledgeFileInfo(baseDir, path))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("读取知识库目录失败: " + baseDir, e);
        }
    }

    @Override
    public KnowledgeIngestResult ingestFiles(List<String> fileNames) {
        KnowledgeIngestResult result = new KnowledgeIngestResult();

        if (CollectionUtils.isEmpty(fileNames)) {
            result.getMessages().add("请选择要导入的知识库文件");
            return result;
        }

        // 去重，防止重复导入同一个文件
        Set<String> uniqueFileNames = fileNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        result.setRequestedCount(uniqueFileNames.size());

        Path baseDir = resolveKnowledgeDir();
        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            result.setFailedCount(uniqueFileNames.size());
            result.getFailedFiles().addAll(uniqueFileNames);
            result.getMessages().add("知识库目录不存在: " + baseDir);
            return result;
        }

        // 构建向量入库器
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(ragDocumentSplitter)
                .build();

        for (String fileName : uniqueFileNames) {
            try {
                Path filePath = resolveKnowledgeFile(baseDir, fileName);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    addFailedResult(result, fileName, "文件不存在: " + fileName);
                    continue;
                }
                if (!isSupportedFile(filePath)) {
                    addFailedResult(result, fileName, "文件类型不支持: " + fileName);
                    continue;
                }

                // 加载文档并导入向量库
                Document document = loadDocument(filePath);
                ingestor.ingest(document);
                result.getSuccessFiles().add(fileName);
                result.getMessages().add("导入成功: " + fileName);
            } catch (Exception e) {
                addFailedResult(result, fileName, "导入失败: " + fileName + "，原因: " + e.getMessage());
            }
        }

        result.setSuccessCount(result.getSuccessFiles().size());
        result.setFailedCount(result.getFailedFiles().size());
        return result;
    }

    private void addFailedResult(KnowledgeIngestResult result, String fileName, String message) {
        result.getFailedFiles().add(fileName);
        result.getMessages().add(message);
    }

    private KnowledgeFileInfo toKnowledgeFileInfo(Path baseDir, Path path) {
        KnowledgeFileInfo knowledgeFileInfo = new KnowledgeFileInfo();
        knowledgeFileInfo.setFileName(toRelativeFileName(baseDir, path));
        knowledgeFileInfo.setExtension(getExtension(path));
        try {
            knowledgeFileInfo.setSize(Files.size(path));
            FileTime lastModifiedTime = Files.getLastModifiedTime(path);
            LocalDateTime lastModified = LocalDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.systemDefault());
            knowledgeFileInfo.setLastModified(lastModified.format(DATE_TIME_FORMATTER));
        } catch (IOException e) {
            knowledgeFileInfo.setLastModified("");
        }
        return knowledgeFileInfo;
    }

    private Document loadDocument(Path filePath) {
        String extension = getExtension(filePath);
        if ("pdf".equals(extension)) {
            // PDF 文件使用 PDF 解析器
            return FileSystemDocumentLoader.loadDocument(filePath.toString(), apachePdfBoxDocumentParser);
        }
        // md、txt 文件使用文本解析器
        return FileSystemDocumentLoader.loadDocument(filePath.toString(), textDocumentParser);
    }

    private Path resolveKnowledgeDir() {
        Path path = Paths.get(ragProperties.getKnowledgeDir());
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        return path.normalize();
    }

    private Path resolveKnowledgeFile(Path baseDir, String fileName) {
        Path resolvedPath = baseDir.resolve(fileName).normalize();
        if (!resolvedPath.startsWith(baseDir)) {
            throw new IllegalArgumentException("非法文件路径: " + fileName);
        }
        return resolvedPath;
    }

    private boolean isSupportedFile(Path path) {
        String extension = getExtension(path);
        if (!StringUtils.hasText(extension)) {
            return false;
        }
        return ragProperties.getAllowedExtensions().stream()
                .map(this::normalizeExtension)
                .anyMatch(extension::equals);
    }

    private String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return normalizeExtension(fileName.substring(dotIndex + 1));
    }

    private String normalizeExtension(String extension) {
        return extension.trim().toLowerCase();
    }

    private String toRelativeFileName(Path baseDir, Path path) {
        return baseDir.relativize(path).toString().replace('\\', '/');
    }
}
