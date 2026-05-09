package com.exe101.conversation.repository;

import com.exe101.conversation.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IMessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    boolean existsByIdAndConversationId(Long id, Long conversationId);

    @Query("""
            SELECT message
            FROM Message message
            WHERE message.conversationId = :conversationId
              AND (:cursor IS NULL OR message.id < :cursor)
            ORDER BY message.id DESC
            """)
    List<Message> findLatestForScroll(
            @Param("conversationId") Long conversationId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
