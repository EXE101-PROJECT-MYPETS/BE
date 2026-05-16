package com.exe101.ai.repository;

import com.exe101.ai.entity.AiPetChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiPetChatConversationRepository extends JpaRepository<AiPetChatConversation, Long> {
    Optional<AiPetChatConversation> findByIdAndPetId(Long id, Long petId);

    List<AiPetChatConversation> findByPetIdOrderByUpdatedAtDesc(Long petId);

    List<AiPetChatConversation> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE AiPetChatConversation c SET c.title = c.title WHERE c.id = :conversationId")
    int touch(@Param("conversationId") Long conversationId);
}
