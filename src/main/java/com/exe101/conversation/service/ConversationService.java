package com.exe101.conversation.service;

import com.exe101.common.IService;
import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.dto.MessageCreateRequest;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.entity.Conversation;
import com.exe101.conversation.entity.Message;
import com.exe101.conversation.entity.MessageSenderType;
import com.exe101.conversation.exception.ConversationNotFound;
import com.exe101.conversation.exception.ConversationValidationException;
import com.exe101.conversation.mapper.ConversationMapper;
import com.exe101.conversation.mapper.MessageMapper;
import com.exe101.conversation.repository.IConversationRepository;
import com.exe101.conversation.repository.IMessageRepository;
import com.exe101.shopMember.dto.ShopMemberDTO;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.entity.UserStatus;
import com.exe101.user.exception.UserNotFound;
import com.exe101.user.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService implements IService<Conversation, ConversationDTO, Long> {

    private final IConversationRepository conversationRepository;
    private final IMessageRepository messageRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final IUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDTO> getAll() {
        return conversationRepository.findAll().stream().map(ConversationMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationDTO> getAllByShopId(Long shopId) {
        return conversationRepository.findSummariesByShopId(shopId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDTO getById(Long id) {
        return conversationRepository.findById(id)
                .map(ConversationMapper::toDTO)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Khong tim thay cuoc tro chuyen"));
    }

    @Transactional(readOnly = true)
    public ConversationDTO getById(Long shopId, Long id) {
        return conversationRepository.findSummaryByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Khong tim thay cuoc tro chuyen"));
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessages(Long shopId, Long conversationId) {
        Conversation conversation = findByIdAndShopId(conversationId, shopId);
        return messageRepository.findByConversationIdOrderByIdAsc(conversation.getId()).stream()
                .map(MessageMapper::toDTO)
                .toList();
    }

    @Transactional
    public MessageDTO sendMessage(Long shopId, Long conversationId, MessageCreateRequest request) {
        Conversation conversation = findByIdAndShopId(conversationId, shopId);
        return saveMessage(conversation, request);
    }

    @Transactional
    public MessageDTO sendMessage(Long conversationId, MessageCreateRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
        return saveMessage(conversation, request);
    }

    @Override
    @Transactional
    public ConversationDTO create(ConversationDTO dto) {
        validateUserTarget(dto.getUserId());
        validateConversationUniqueness(dto.getShopId(), dto.getUserId(), null);
        return ConversationMapper.toDTO(conversationRepository.save(ConversationMapper.toEntity(dto)));
    }

    @Override
    @Transactional
    public ConversationDTO update(Long id, ConversationDTO dto) {
        Conversation entity = findByIdAndShopId(id, dto.getShopId());
        validateUserTarget(dto.getUserId());
        validateConversationUniqueness(dto.getShopId(), dto.getUserId(), id);
        entity.setShopId(dto.getShopId());
        entity.setUserId(dto.getUserId());
        if (dto.getShopLastReadMessageId() != null) {
            entity.setShopLastReadMessageId(dto.getShopLastReadMessageId());
        }
        if (dto.getUserLastReadMessageId() != null) {
            entity.setUserLastReadMessageId(dto.getUserLastReadMessageId());
        }
        return ConversationMapper.toDTO(conversationRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!conversationRepository.existsById(id)) {
            throw new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện");
        }
        conversationRepository.deleteById(id);
    }

    @Transactional
    public void delete(Long shopId, Long id) {
        Conversation conversation = findByIdAndShopId(id, shopId);
        conversationRepository.delete(conversation);
    }

    private Conversation findByIdAndShopId(Long id, Long shopId) {
        return conversationRepository.findByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
    }

    private MessageDTO saveMessage(Conversation conversation, MessageCreateRequest request) {
        if (request.getSenderType() == MessageSenderType.USER) {
            return saveUserMessage(conversation, request);
        }
        if (request.getSenderType() == MessageSenderType.SHOP) {
            return saveShopMessage(conversation, request);
        }
        throw new ConversationValidationException("MessageSenderTypeInvalid", "Loại người gửi không hợp lệ");
    }

    private MessageDTO saveUserMessage(Conversation conversation, MessageCreateRequest request) {
        if (request.getSenderUserId() != null && !request.getSenderUserId().equals(conversation.getUserId())) {
            throw new ConversationValidationException(
                    "MessageSenderInvalid",
                    "User gui tin khong thuoc cuoc tro chuyen nay"
            );
        }

        return saveMessageEntity(
                conversation,
                MessageSenderType.USER,
                conversation.getUserId(),
                request.getBody()
        );
    }

    private MessageDTO saveShopMessage(Conversation conversation, MessageCreateRequest request) {
        Long senderUserId = resolveShopSenderUserId(conversation.getShopId(), request.getSenderUserId());

        return saveMessageEntity(
                conversation,
                MessageSenderType.SHOP,
                senderUserId,
                request.getBody()
        );
    }

    private MessageDTO saveMessageEntity(
            Conversation conversation,
            MessageSenderType senderType,
            Long senderUserId,
            String body
    ) {
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setShopId(conversation.getShopId());
        message.setSenderType(senderType);
        message.setSenderUserId(senderUserId);
        message.setBody(body.trim());
        return MessageMapper.toDTO(messageRepository.save(message));
    }

    private void validateUserTarget(Long userId) {
        if (userId == null) {
            throw new ConversationValidationException(
                    "ConversationUserRequired",
                    "Cuoc tro chuyen can co userId"
            );
        }
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UserNotFound(
                    "UserNotFound",
                    "Khong tim thay user active de tao cuoc tro chuyen"
            );
        }
    }

    private void validateConversationUniqueness(Long shopId, Long userId, Long currentConversationId) {
        if (currentConversationId == null) {
            if (conversationRepository.existsByShopIdAndUserId(shopId, userId)) {
                throw new ConversationValidationException(
                        "ConversationDuplicate",
                        "Shop da co cuoc tro chuyen voi user nay"
                );
            }
            return;
        }

        conversationRepository.findByShopIdAndUserId(shopId, userId)
                .filter(conversation -> !conversation.getId().equals(currentConversationId))
                .ifPresent(conversation -> {
                    throw new ConversationValidationException(
                            "ConversationDuplicate",
                            "Shop da co cuoc tro chuyen voi user nay"
                    );
                });
    }

    private Long resolveShopSenderUserId(Long shopId, Long requestedUserId) {
        if (requestedUserId != null) {
            if (!shopMemberRepository.existsByShopIdAndUserIdAndStatus(shopId, requestedUserId, MemberStatus.ACTIVE)) {
                throw new ConversationValidationException(
                        "MessageSenderInvalid",
                        "Account gửi tin không phải account active của shop"
                );
            }
            return requestedUserId;
        }

        List<ShopMemberDTO> activeAccounts = shopMemberRepository.findByShopIdAndStatusForDisplay(shopId, MemberStatus.ACTIVE);
        if (activeAccounts.isEmpty()) {
            throw new ConversationValidationException(
                    "ShopAccountNotFound",
                    "Shop chưa có account active để gửi tin nhắn"
            );
        }
        return activeAccounts.get(0).getUserId();
    }
}
