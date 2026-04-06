package com.exe101.conversation.service;

import com.exe101.common.IService;
import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.entity.Conversation;
import com.exe101.conversation.exception.ConversationNotFound;
import com.exe101.conversation.mapper.ConversationMapper;
import com.exe101.conversation.repository.IConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService implements IService<Conversation, ConversationDTO, Long> {

    private final IConversationRepository conversationRepository;

    @Override
    public List<ConversationDTO> getAll() {
        return conversationRepository.findAll().stream().map(ConversationMapper::toDTO).toList();
    }

    @Override
    public ConversationDTO getById(Long id) {
        return conversationRepository.findById(id)
                .map(ConversationMapper::toDTO)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Conversation not found"));
    }

    @Override
    public ConversationDTO create(ConversationDTO dto) {
        return ConversationMapper.toDTO(conversationRepository.save(ConversationMapper.toEntity(dto)));
    }

    @Override
    public ConversationDTO update(Long id, ConversationDTO dto) {
        Conversation entity = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Conversation not found"));
        entity.setShopId(dto.getShopId());
        entity.setCustomerId(dto.getCustomerId());
        return ConversationMapper.toDTO(conversationRepository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!conversationRepository.existsById(id)) {
            throw new ConversationNotFound("ConversationNotFound", "Conversation not found");
        }
        conversationRepository.deleteById(id);
    }
}
