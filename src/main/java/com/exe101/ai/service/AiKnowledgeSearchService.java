package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiKnowledgeSearchService {

    private final AiEmbeddingService aiEmbeddingService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<AiKnowledgeSearchResult> search(String query, int limit) {
        int normalizedLimit = Math.max(1, limit);
        String embeddingText = aiEmbeddingService.toPgVectorString(aiEmbeddingService.embed(query));
        return hybridSearch(query, embeddingText, Math.max(30, normalizedLimit), normalizedLimit);
    }

    @Transactional(readOnly = true)
    public List<AiKnowledgeSearchResult> hybridSearch(
            String rewrittenQuery,
            String embeddingText,
            int candidateLimit,
            int finalLimit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("embedding", embeddingText)
                .addValue("keywordQuery", rewrittenQuery == null ? "" : rewrittenQuery)
                .addValue("candidateLimit", Math.max(1, candidateLimit))
                .addValue("finalLimit", Math.max(1, finalLimit));

        return jdbcTemplate.query(
                """
                WITH vector_results AS (
                    SELECT
                        id,
                        topic,
                        source_type,
                        source_id,
                        title,
                        content,
                        metadata,
                        1 - (embedding <=> CAST(:embedding AS vector)) AS vector_score,
                        0.0 AS keyword_score
                    FROM prod.ai_knowledge_embeddings
                    ORDER BY embedding <=> CAST(:embedding AS vector)
                    LIMIT :candidateLimit
                ),
                keyword_results AS (
                    SELECT
                        id,
                        topic,
                        source_type,
                        source_id,
                        title,
                        content,
                        metadata,
                        0.0 AS vector_score,
                        ts_rank_cd(
                            search_text,
                            plainto_tsquery('simple', unaccent(:keywordQuery))
                        ) AS keyword_score
                    FROM prod.ai_knowledge_embeddings
                    WHERE search_text @@ plainto_tsquery('simple', unaccent(:keywordQuery))
                    ORDER BY ts_rank_cd(
                        search_text,
                        plainto_tsquery('simple', unaccent(:keywordQuery))
                    ) DESC
                    LIMIT :candidateLimit
                ),
                combined AS (
                    SELECT * FROM vector_results
                    UNION ALL
                    SELECT * FROM keyword_results
                ),
                merged AS (
                    SELECT
                        id,
                        max(topic) AS topic,
                        max(source_type) AS source_type,
                        max(source_id) AS source_id,
                        max(title) AS title,
                        max(content) AS content,
                        max(metadata::text)::jsonb AS metadata,
                        max(vector_score) AS vector_score,
                        max(keyword_score) AS keyword_score
                    FROM combined
                    GROUP BY id
                )
                SELECT
                    id,
                    topic,
                    source_type,
                    source_id,
                    title,
                    content,
                    metadata,
                    vector_score,
                    keyword_score,
                    (0.7 * vector_score + 0.3 * keyword_score) AS hybrid_score
                FROM merged
                ORDER BY hybrid_score DESC
                LIMIT :finalLimit
                """,
                params,
                this::mapSearchResult
        );
    }

    private AiKnowledgeSearchResult mapSearchResult(ResultSet rs, int rowNum) throws SQLException {
        return AiKnowledgeSearchResult.builder()
                .id(rs.getObject("id", UUID.class))
                .topic(rs.getString("topic"))
                .sourceType(rs.getString("source_type"))
                .sourceId(rs.getObject("source_id") == null ? null : rs.getString("source_id"))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .metadata(rs.getString("metadata"))
                .vectorScore(rs.getObject("vector_score") == null ? 0.0 : rs.getDouble("vector_score"))
                .keywordScore(rs.getObject("keyword_score") == null ? 0.0 : rs.getDouble("keyword_score"))
                .hybridScore(rs.getObject("hybrid_score") == null ? 0.0 : rs.getDouble("hybrid_score"))
                .rerankScore(0.0)
                .build();
    }
}
