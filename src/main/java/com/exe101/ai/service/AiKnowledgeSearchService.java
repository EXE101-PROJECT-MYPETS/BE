package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeSearchResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiKnowledgeSearchService {

    private final GeminiClientService geminiClientService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<AiKnowledgeSearchResult> search(String query, int limit) {
        int normalizedLimit = Math.max(1, limit);
        String vectorLiteral = toVectorLiteral(geminiClientService.embedText(query));

        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    topic,
                    source_type,
                    source_id,
                    title,
                    content,
                    metadata,
                    1 - (embedding OPERATOR(prod.<=>) CAST(? AS prod.vector)) AS similarity
                FROM prod.ai_knowledge_embeddings
                ORDER BY embedding OPERATOR(prod.<=>) CAST(? AS prod.vector)
                LIMIT ?
                """,
                this::mapSearchResult,
                vectorLiteral,
                vectorLiteral,
                normalizedLimit
        );
    }

    private AiKnowledgeSearchResult mapSearchResult(ResultSet rs, int rowNum) throws SQLException {
        return new AiKnowledgeSearchResult(
                rs.getObject("id", UUID.class),
                rs.getString("topic"),
                rs.getString("source_type"),
                rs.getObject("source_id") == null ? null : rs.getLong("source_id"),
                rs.getString("title"),
                rs.getString("content"),
                readJson(rs.getString("metadata")),
                rs.getObject("similarity") == null ? null : rs.getDouble("similarity")
        );
    }

    private JsonNode readJson(String value) {
        try {
            return value == null ? objectMapper.createObjectNode() : objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể parse metadata JSON", ex);
        }
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding.get(i));
        }
        builder.append(']');
        return builder.toString();
    }
}
