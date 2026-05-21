package com.exe101.ai.repository;

import com.exe101.ai.entity.AiPetChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiPetChatMessageRepository extends JpaRepository<AiPetChatMessage, Long> {
    List<AiPetChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    List<AiPetChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
