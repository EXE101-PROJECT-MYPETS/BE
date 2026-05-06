package com.exe101.conversation.repository;

import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByShopIdOrderByIdDesc(Long shopId);

    Optional<Conversation> findByIdAndShopId(Long id, Long shopId);

    Optional<Conversation> findByShopIdAndUserId(Long shopId, Long userId);

    boolean existsByShopIdAndUserId(Long shopId, Long userId);

    @Query("""
            SELECT new com.exe101.conversation.dto.ConversationDTO(
                conversation.id,
                conversation.shopId,
                conversation.userId,
                targetUser.fullName,
                targetUser.phone,
                targetUser.email,
                targetUser.avatarUrlPreview,
                lastMessage.id,
                lastMessage.body,
                lastMessage.senderType,
                lastMessage.createdAt,
                (
                    SELECT COUNT(unreadMessage.id)
                    FROM Message unreadMessage
                    WHERE unreadMessage.conversationId = conversation.id
                      AND unreadMessage.senderType = com.exe101.conversation.entity.MessageSenderType.USER
                      AND (
                          conversation.shopLastReadMessageId IS NULL
                          OR unreadMessage.id > conversation.shopLastReadMessageId
                      )
                ),
                conversation.shopLastReadMessageId,
                conversation.userLastReadMessageId,
                conversation.createdAt,
                conversation.updatedAt
            )
            FROM Conversation conversation
            JOIN conversation.user targetUser
            LEFT JOIN Message lastMessage
              ON lastMessage.conversationId = conversation.id
             AND lastMessage.id = (
                 SELECT MAX(latestMessage.id)
                 FROM Message latestMessage
                 WHERE latestMessage.conversationId = conversation.id
             )
            WHERE conversation.shopId = :shopId
            ORDER BY COALESCE(lastMessage.createdAt, conversation.createdAt) DESC, conversation.id DESC
            """)
    List<ConversationDTO> findSummariesByShopId(@Param("shopId") Long shopId);

    @Query("""
            SELECT new com.exe101.conversation.dto.ConversationDTO(
                conversation.id,
                conversation.shopId,
                conversation.userId,
                targetUser.fullName,
                targetUser.phone,
                targetUser.email,
                targetUser.avatarUrlPreview,
                lastMessage.id,
                lastMessage.body,
                lastMessage.senderType,
                lastMessage.createdAt,
                (
                    SELECT COUNT(unreadMessage.id)
                    FROM Message unreadMessage
                    WHERE unreadMessage.conversationId = conversation.id
                      AND unreadMessage.senderType = com.exe101.conversation.entity.MessageSenderType.USER
                      AND (
                          conversation.shopLastReadMessageId IS NULL
                          OR unreadMessage.id > conversation.shopLastReadMessageId
                      )
                ),
                conversation.shopLastReadMessageId,
                conversation.userLastReadMessageId,
                conversation.createdAt,
                conversation.updatedAt
            )
            FROM Conversation conversation
            JOIN conversation.user targetUser
            LEFT JOIN Message lastMessage
              ON lastMessage.conversationId = conversation.id
             AND lastMessage.id = (
                 SELECT MAX(latestMessage.id)
                 FROM Message latestMessage
                 WHERE latestMessage.conversationId = conversation.id
             )
            WHERE conversation.id = :id
              AND conversation.shopId = :shopId
            """)
    Optional<ConversationDTO> findSummaryByIdAndShopId(
            @Param("id") Long id,
            @Param("shopId") Long shopId
    );
}
