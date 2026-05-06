package com.exe101.conversation.mapper;

import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.entity.Conversation;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public static ConversationDTO toDTO(Conversation entity) {
        if (entity == null) return null;
        return new ConversationDTO(
                entity.getId(),
                entity.getShopId(),
                entity.getUserId(),
                entity.getUser() != null ? entity.getUser().getFullName() : null,
                entity.getUser() != null ? entity.getUser().getPhone() : null,
                entity.getUser() != null ? entity.getUser().getEmail() : null,
                entity.getUser() != null ? entity.getUser().getAvatarUrlPreview() : null,
                null,
                null,
                null,
                null,
                0L,
                entity.getShopLastReadMessageId(),
                entity.getUserLastReadMessageId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Conversation toEntity(ConversationDTO dto) {
        if (dto == null) return null;
        Conversation entity = new Conversation();
        entity.setShopId(dto.getShopId());
        entity.setUserId(dto.getUserId());
        entity.setShopLastReadMessageId(dto.getShopLastReadMessageId());
        entity.setUserLastReadMessageId(dto.getUserLastReadMessageId());
        return entity;
    }
}
