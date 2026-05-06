package com.exe101.conversation.mapper;

import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public static MessageDTO toDTO(Message entity) {
        if (entity == null) return null;
        return new MessageDTO(
                entity.getId(),
                entity.getConversationId(),
                entity.getShopId(),
                entity.getSenderType(),
                entity.getSenderUserId(),
                entity.getBody(),
                entity.getCreatedAt()
        );
    }

    public static Message toEntity(MessageDTO dto) {
        if (dto == null) return null;
        Message entity = new Message();
        entity.setConversationId(dto.getConversationId());
        entity.setShopId(dto.getShopId());
        entity.setSenderType(dto.getSenderType());
        entity.setSenderUserId(dto.getSenderUserId());
        entity.setBody(dto.getBody());
        return entity;
    }
}
