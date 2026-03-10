package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.KnowledgeIngestResult;
import com.atguigu.java.ai.langchain4j.service.KnowledgeService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

@SpringBootTest
public class EmbeddingTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    public void testEmbeddingModel(){
        Response<Embedding> embed = embeddingModel.embed("你好");

        System.out.println("向量维度：" + embed.content().vector().length);
        System.out.println("向量输出：" + embed);
    }

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 将文本转换成向量，然后存储到 Pinecone 中
     *
     * 参考：
     * https://docs.langchain4j.dev/tutorials/embedding-stores
     */
    @Test
    public void testPineconeEmbeded() {

        // 将文本转换成向量
        TextSegment segment1 = TextSegment.from("我喜欢羽毛球");
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        // 存入向量数据库
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("今天天气很好");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);
    }

    /**
     * Pinecone 相似度匹配
     */
    @Test
    public void embeddingSearch() {

        // 提问，并将问题转换成向量数据
        Embedding queryEmbedding = embeddingModel.embed("你最喜欢的运动是什么？").content();
        // 创建搜索请求对象
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(1) // 匹配最相似的一条记录
                //.minScore(0.8)
                .build();

        // 根据搜索请求在向量存储中进行相似度搜索
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        // searchResult.matches()：获取搜索结果中的匹配项列表
        // get(0)：获取第一条匹配结果
        EmbeddingMatch<TextSegment> embeddingMatch = searchResult.matches().get(0);

        // 获取匹配结果的相似度分数
        System.out.println(embeddingMatch.score()); // 0.8144288515898701

        // 返回文本结果
        System.out.println(embeddingMatch.embedded().text());
    }

    @Disabled("Manual test for knowledge ingestion")
    @Test
    public void testUploadKnowledgeLibrary() {
        KnowledgeIngestResult ingestResult = knowledgeService.ingestFiles(Arrays.asList(
                "医院信息.md",
                "科室信息.md",
                "神经内科.md"
        ));
        System.out.println(ingestResult);
    }
}
