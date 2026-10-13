package org.xhy.domain.rag.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xhy.domain.rag.constant.SearchType;
import org.xhy.domain.rag.dto.HybridSearchConfig;
import org.xhy.domain.rag.model.DocumentUnitEntity;
import org.xhy.domain.rag.model.ModelConfig;
import org.xhy.domain.rag.model.VectorStoreResult;
import org.xhy.domain.rag.repository.DocumentUnitRepository;
import org.xhy.infrastructure.rag.factory.EmbeddingModelFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridSearchDomainServiceTest {

    @Mock
    private EmbeddingDomainService embeddingDomainService;

    @Mock
    private KeywordSearchDomainService keywordSearchDomainService;

    @Mock
    private DocumentUnitRepository documentUnitRepository;

    @Mock
    private RerankDomainService rerankDomainService;

    @Mock
    private HyDEDomainService hydeDomainService;

    @InjectMocks
    private HybridSearchDomainService hybridSearchDomainService;

    @Test
    void shouldRetrieveWithOriginalAndHydeQueriesAndRerankWithOriginalQuery() {
        String originalQuery = "如何实现Agent工具调用";
        String hypotheticalDocument = "Agent通过工具描述、参数校验和执行器完成外部工具调用";
        List<String> dataSetIds = List.of("dataset-1");
        EmbeddingModelFactory.EmbeddingConfig embeddingConfig = new EmbeddingModelFactory.EmbeddingConfig(
                "test-key", "http://embedding.test", "test-model");
        ModelConfig chatModelConfig = new ModelConfig();
        HybridSearchConfig config = HybridSearchConfig.builder(dataSetIds, originalQuery).maxResults(3)
                .candidateMultiplier(2).enableRerank(true).embeddingConfig(embeddingConfig)
                .chatModelConfig(chatModelConfig).build();

        VectorStoreResult originalResult = vectorResult("doc-a", "original result");
        VectorStoreResult hydeResult = vectorResult("doc-b", "hyde result");
        VectorStoreResult keywordResult = vectorResult("doc-a", "keyword result");

        when(hydeDomainService.generateHypotheticalDocument(originalQuery, chatModelConfig))
                .thenReturn(hypotheticalDocument);
        when(embeddingDomainService.vectorSearch(eq(dataSetIds), eq(originalQuery), eq(6), eq(0.7), eq(false),
                eq(2), same(embeddingConfig))).thenReturn(List.of(originalResult));
        when(embeddingDomainService.vectorSearch(eq(dataSetIds), eq(hypotheticalDocument), eq(6), eq(0.7),
                eq(false), eq(2), same(embeddingConfig))).thenReturn(List.of(hydeResult));
        when(keywordSearchDomainService.keywordSearch(dataSetIds, originalQuery, 6))
                .thenReturn(List.of(keywordResult));
        when(rerankDomainService.rerank(anyList(), eq(originalQuery))).thenReturn(List.of(1, 0));
        when(documentUnitRepository.selectList(any())).thenReturn(
                List.of(document("doc-a"), document("doc-b")));

        List<DocumentUnitEntity> results = hybridSearchDomainService.hybridSearch(config);

        assertThat(config.getQuestion()).isEqualTo(originalQuery);
        assertThat(results).extracting(DocumentUnitEntity::getId).containsExactly("doc-b", "doc-a");
        verify(hydeDomainService).generateHypotheticalDocument(originalQuery, chatModelConfig);
        verify(keywordSearchDomainService).keywordSearch(dataSetIds, originalQuery, 6);
        verify(rerankDomainService).rerank(anyList(), eq(originalQuery));
    }

    private VectorStoreResult vectorResult(String documentId, String text) {
        return new VectorStoreResult(documentId + "-embedding", text, Map.of("DOCUMENT_ID", documentId), 0.9,
                SearchType.VECTOR);
    }

    private DocumentUnitEntity document(String id) {
        DocumentUnitEntity document = new DocumentUnitEntity();
        document.setId(id);
        return document;
    }
}
