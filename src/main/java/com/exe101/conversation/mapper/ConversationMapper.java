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
                entity.getCustomerId(),
                entity.getCreatedAt()
        );
    }

    public static Conversation toEntity(ConversationDTO dto) {
        if (dto == null) return null;
        Conversation entity = new Conversation();
        entity.setShopId(dto.getShopId());
        entity.setCustomerId(dto.getCustomerId());
        return entity;
    }
}
