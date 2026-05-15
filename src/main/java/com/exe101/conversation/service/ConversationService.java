package com.exe101.conversation.service;

import com.exe101.common.IService;
import com.exe101.common.ScrollResponse;
import com.exe101.conversation.dto.ConversationDTO;
import com.exe101.conversation.dto.CustomerConversationDTO;
import com.exe101.conversation.dto.MessageCreateRequest;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.dto.ReadReceiptDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService implements IService<Conversation, ConversationDTO, Long> {

    private static final int MAX_MESSAGE_SCROLL_SIZE = 50;

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
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
    }

    @Transactional(readOnly = true)
    public ConversationDTO getById(Long shopId, Long id) {
        return conversationRepository.findSummaryByIdAndShopId(id, shopId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
    }

    @Transactional(readOnly = true)
    public ScrollResponse<MessageDTO> getMessages(Long shopId, Long conversationId, Long cursor, int size) {
        Conversation conversation = findByIdAndShopId(conversationId, shopId);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_MESSAGE_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;

        List<Message> messages = messageRepository.findLatestForScroll(
                conversation.getId(),
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = messages.size() > normalizedSize;
        List<Message> content = new ArrayList<>(messages.stream()
                .limit(normalizedSize)
                .toList());
        Collections.reverse(content);

        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(0).getId()
                : null;

        return ScrollResponse.of(content.stream()
                .map(MessageMapper::toDTO)
                .toList(), normalizedSize, nextCursor, hasNext);
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

    @Transactional
    public ReadReceiptDTO markUserRead(Long conversationId, Long readerUserId, Long lastReadMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
        if (!conversation.getUserId().equals(readerUserId)) {
            throw new ConversationValidationException(
                    "ConversationReaderInvalid",
                    "Người dùng không thuộc cuộc trò chuyện này"
            );
        }
        validateReadMessage(conversation.getId(), lastReadMessageId);
        if (conversation.getUserLastReadMessageId() == null
                || lastReadMessageId > conversation.getUserLastReadMessageId()) {
            conversation.setUserLastReadMessageId(lastReadMessageId);
            conversationRepository.save(conversation);
        }
        return toReadReceiptDTO(conversation, MessageSenderType.USER, readerUserId, conversation.getUserLastReadMessageId());
    }

    @Transactional
    public ReadReceiptDTO markShopRead(Long shopId, Long conversationId, Long readerUserId, Long lastReadMessageId) {
        Conversation conversation = findByIdAndShopId(conversationId, shopId);
        validateShopReaderUserId(shopId, readerUserId);
        validateReadMessage(conversation.getId(), lastReadMessageId);
        if (conversation.getShopLastReadMessageId() == null
                || lastReadMessageId > conversation.getShopLastReadMessageId()) {
            conversation.setShopLastReadMessageId(lastReadMessageId);
            conversationRepository.save(conversation);
        }
        return toReadReceiptDTO(conversation, MessageSenderType.SHOP, readerUserId, conversation.getShopLastReadMessageId());
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
                    "Người dùng gửi tin không thuộc cuộc trò chuyện này"
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

    private void validateReadMessage(Long conversationId, Long lastReadMessageId) {
        if (!messageRepository.existsByIdAndConversationId(lastReadMessageId, conversationId)) {
            throw new ConversationValidationException(
                    "ReadMessageInvalid",
                    "Tin nhắn đã đọc không thuộc cuộc trò chuyện này"
            );
        }
    }

    private void validateShopReaderUserId(Long shopId, Long readerUserId) {
        if (readerUserId == null
                || !shopMemberRepository.existsByShopIdAndUserIdAndStatus(shopId, readerUserId, MemberStatus.ACTIVE)) {
            throw new ConversationValidationException(
                    "ConversationReaderInvalid",
                    "Tài khoản đọc tin không phải tài khoản đang hoạt động của shop"
            );
        }
    }

    private ReadReceiptDTO toReadReceiptDTO(
            Conversation conversation,
            MessageSenderType readerType,
            Long readerUserId,
            Long lastReadMessageId
    ) {
        return new ReadReceiptDTO(
                conversation.getId(),
                conversation.getShopId(),
                readerType,
                readerUserId,
                lastReadMessageId,
                conversation.getShopLastReadMessageId(),
                conversation.getUserLastReadMessageId()
        );
    }

    private void validateUserTarget(Long userId) {
        if (userId == null) {
            throw new ConversationValidationException(
                    "ConversationUserRequired",
                    "Cuộc trò chuyện cần có userId"
            );
        }
        if (!userRepository.existsByIdAndStatus(userId, UserStatus.ACTIVE)) {
            throw new UserNotFound(
                    "UserNotFound",
                    "Không tìm thấy người dùng đang hoạt động để tạo cuộc trò chuyện"
            );
        }
    }

    private void validateConversationUniqueness(Long shopId, Long userId, Long currentConversationId) {
        if (currentConversationId == null) {
            if (conversationRepository.existsByShopIdAndUserId(shopId, userId)) {
                throw new ConversationValidationException(
                        "ConversationDuplicate",
                        "Shop đã có cuộc trò chuyện với người dùng này"
                );
            }
            return;
        }

        conversationRepository.findByShopIdAndUserId(shopId, userId)
                .filter(conversation -> !conversation.getId().equals(currentConversationId))
                .ifPresent(conversation -> {
                    throw new ConversationValidationException(
                            "ConversationDuplicate",
                            "Shop đã có cuộc trò chuyện với người dùng này"
                    );
                });
    }

    private Long resolveShopSenderUserId(Long shopId, Long requestedUserId) {
        if (requestedUserId != null) {
            if (!shopMemberRepository.existsByShopIdAndUserIdAndStatus(shopId, requestedUserId, MemberStatus.ACTIVE)) {
                throw new ConversationValidationException(
                        "MessageSenderInvalid",
                        "Tài khoản gửi tin không phải tài khoản đang hoạt động của shop"
                );
            }
            return requestedUserId;
        }

        List<ShopMemberDTO> activeAccounts = shopMemberRepository.findByShopIdAndStatusForDisplay(shopId, MemberStatus.ACTIVE);
        if (activeAccounts.isEmpty()) {
            throw new ConversationValidationException(
                    "ShopAccountNotFound",
                    "Shop chưa có tài khoản đang hoạt động để gửi tin nhắn"
            );
        }
        return activeAccounts.get(0).getUserId();
    }

    // --- CUSTOMER (USER) FACING METHODS ---

    @Transactional(readOnly = true)
    public List<CustomerConversationDTO> getAllByUserId(Long userId) {
        return conversationRepository.findSummariesByUserId(userId).stream()
            .map(this::sanitizeShopAvatar)
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerConversationDTO getByIdForUser(Long userId, Long id) {
        return conversationRepository.findSummaryByIdAndUserId(id, userId)
            .map(this::sanitizeShopAvatar)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
    }

    @Transactional(readOnly = true)
    public ScrollResponse<MessageDTO> getMessagesForUser(Long userId, Long conversationId, Long cursor, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
        
        if (!conversation.getUserId().equals(userId)) {
            throw new ConversationValidationException("ConversationAccessDenied", "Bạn không có quyền truy cập cuộc trò chuyện này");
        }

        int normalizedSize = Math.min(Math.max(size, 1), MAX_MESSAGE_SCROLL_SIZE);
        Long normalizedCursor = cursor != null && cursor > 0 ? cursor : null;

        List<Message> messages = messageRepository.findLatestForScroll(
                conversation.getId(),
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );

        boolean hasNext = messages.size() > normalizedSize;
        List<Message> content = new ArrayList<>(messages.stream()
                .limit(normalizedSize)
                .toList());
        Collections.reverse(content);

        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(0).getId()
                : null;

        return ScrollResponse.of(content.stream()
                .map(MessageMapper::toDTO)
                .toList(), normalizedSize, nextCursor, hasNext);
    }

    @Transactional
    public MessageDTO sendMessageAsUser(Long userId, Long conversationId, MessageCreateRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFound("ConversationNotFound", "Không tìm thấy cuộc trò chuyện"));
        
        if (!conversation.getUserId().equals(userId)) {
            throw new ConversationValidationException("ConversationAccessDenied", "Bạn không có quyền gửi tin nhắn vào cuộc trò chuyện này");
        }
        
        request.setSenderUserId(userId);
        request.setSenderType(MessageSenderType.USER);
        
        return saveUserMessage(conversation, request);
    }

    @Transactional
    public CustomerConversationDTO getOrCreateConversation(Long userId, Long shopId) {
        validateUserTarget(userId);
        
        // Return existing if present
        var existing = conversationRepository.findByShopIdAndUserId(shopId, userId);
        if (existing.isPresent()) {
            return getByIdForUser(userId, existing.get().getId());
        }
        
        // Create new
        Conversation conversation = new Conversation();
        conversation.setShopId(shopId);
        conversation.setUserId(userId);
        conversation = conversationRepository.save(conversation);
        
        return getByIdForUser(userId, conversation.getId());
    }

    private CustomerConversationDTO sanitizeShopAvatar(CustomerConversationDTO dto) {
        if (dto == null) {
            return null;
        }
        String avatarUrl = dto.getShopAvatarUrl();
        if (avatarUrl == null) {
            return dto;
        }
        String normalized = avatarUrl.trim();
        if (normalized.isEmpty()) {
            dto.setShopAvatarUrl(null);
            return dto;
        }
        String lower = normalized.toLowerCase();
        if (lower.startsWith("/uploads/")
                || lower.startsWith("uploads/")
                || lower.contains(":8080/uploads/")) {
            dto.setShopAvatarUrl(null);
        }
        return dto;
    }
}
