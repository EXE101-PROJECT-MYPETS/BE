package com.exe101.conversation.mapper;

import com.exe101.conversation.dto.ConversationMemberDTO;
import com.exe101.conversation.entity.ConversationMember;
import org.springframework.stereotype.Component;

@Component
public class ConversationMemberMapper {

    public static ConversationMemberDTO toDTO(ConversationMember entity) {
        if (entity == null) return null;
        return new ConversationMemberDTO(
                entity.getId(),
                entity.getConversationId(),
                entity.getMemberType(),
                entity.getCustomerId(),
                entity.getUserId(),
                entity.getLastReadMessageId(),
                entity.getCreatedAt()
        );
    }

    public static ConversationMember toEntity(ConversationMemberDTO dto) {
        if (dto == null) return null;
        ConversationMember entity = new ConversationMember();
        entity.setConversationId(dto.getConversationId());
        entity.setMemberType(dto.getMemberType());
        entity.setCustomerId(dto.getCustomerId());
        entity.setUserId(dto.getUserId());
        entity.setLastReadMessageId(dto.getLastReadMessageId());
        return entity;
    }
}
