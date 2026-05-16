package com.exe101.ai.service;

import com.exe101.ai.dto.AiKnowledgeCreateRequest;
import com.exe101.ai.dto.AiKnowledgeCreateResponse;
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
public class AiKnowledgeIngestionService {

    private final GeminiClientService geminiClientService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiKnowledgeCreateResponse createKnowledge(AiKnowledgeCreateRequest request) {
        List<Double> embedding = geminiClientService.embedText(request.getContent());
        String vectorLiteral = toVectorLiteral(embedding);
        String metadataJson = toJson(request.getMetadata());

        return jdbcTemplate.queryForObject(
                """
                INSERT INTO prod.ai_knowledge_embeddings (
                    topic,
                    source_type,
                    source_id,
                    title,
                    content,
                    metadata,
                    embedding
                )
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS prod.vector))
                RETURNING id, title, topic
                """,
                this::mapCreateResponse,
                request.getTopic(),
                request.getSourceType().name(),
                request.getSourceId(),
                request.getTitle(),
                request.getContent(),
                metadataJson,
                vectorLiteral
        );
    }

    @Transactional
    public List<AiKnowledgeCreateResponse> createKnowledgeBulk(List<AiKnowledgeCreateRequest> requests) {
        return requests.stream()
                .map(this::createKnowledge)
                .toList();
    }

    private AiKnowledgeCreateResponse mapCreateResponse(ResultSet rs, int rowNum) throws SQLException {
        return new AiKnowledgeCreateResponse(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("topic")
        );
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

    private String toJson(JsonNode metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? objectMapper.createObjectNode() : metadata);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể chuyển metadata sang JSON", ex);
        }
    }
}
