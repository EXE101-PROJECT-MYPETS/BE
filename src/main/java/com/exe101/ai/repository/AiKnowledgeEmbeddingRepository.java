package com.exe101.ai.repository;

import com.exe101.ai.entity.AiKnowledgeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiKnowledgeEmbeddingRepository extends JpaRepository<AiKnowledgeEmbedding, UUID> {
}
