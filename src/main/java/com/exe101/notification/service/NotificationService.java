package com.exe101.notification.service;

import com.exe101.common.ScrollResponse;
import com.exe101.conversation.dto.MessageDTO;
import com.exe101.conversation.entity.Conversation;
import com.exe101.conversation.entity.MessageSenderType;
import com.exe101.conversation.repository.IConversationRepository;
import com.exe101.notification.dto.NotificationRecipientType;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import com.exe101.notification.dto.RealtimeNotificationDTO;
import com.exe101.notification.dto.UnreadCountDTO;
import com.exe101.notification.entity.Notification;
import com.exe101.notification.exception.NotificationAccessDenied;
import com.exe101.notification.exception.NotificationNotFound;
import com.exe101.notification.repository.INotificationRepository;
import com.exe101.shopMember.entity.MemberStatus;
import com.exe101.shopMember.repository.IShopMemberRepository;
import com.exe101.user.repository.IUserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_SCROLL_SIZE = 50;
    private static final int BODY_PREVIEW_MAX_LENGTH = 120;
    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final INotificationRepository notificationRepository;
    private final IConversationRepository conversationRepository;
    private final IShopMemberRepository shopMemberRepository;
    private final IUserRepository userRepository;
    private final NotificationSocketPublisher socketPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public RealtimeNotificationDTO publishToShop(
            Long shopId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            Long actorUserId,
            String title,
            String body,
            Map<String, Object> metadata
    ) {
        return createAndPublish(
                NotificationRecipientType.SHOP,
                null,
                shopId,
                shopId,
                type,
                targetType,
                targetId,
                actorUserId,
                title,
                body,
                metadata
        );
    }

    @Transactional
    public RealtimeNotificationDTO publishToUser(
            Long userId,
            Long shopId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            Long actorUserId,
            String title,
            String body,
            Map<String, Object> metadata
    ) {
        return createAndPublish(
                NotificationRecipientType.USER,
                userId,
                null,
                shopId,
                type,
                targetType,
                targetId,
                actorUserId,
                title,
                body,
                metadata
        );
    }

    @Transactional
    public void publishChatMessage(MessageDTO message) {
        if (message.getSenderType() == MessageSenderType.USER) {
            publishToShop(
                    message.getShopId(),
                    NotificationType.CHAT_MESSAGE,
                    NotificationTargetType.MESSAGE,
                    message.getId(),
                    message.getSenderUserId(),
                    "Tin nhắn mới từ người dùng",
                    toBodyPreview(message.getBody()),
                    buildChatMetadata(message)
            );
            return;
        }

        if (message.getSenderType() == MessageSenderType.SHOP) {
            conversationRepository.findById(message.getConversationId())
                    .map(Conversation::getUserId)
                    .ifPresent(userId -> publishToUser(
                            userId,
                            message.getShopId(),
                            NotificationType.CHAT_MESSAGE,
                            NotificationTargetType.MESSAGE,
                            message.getId(),
                            message.getSenderUserId(),
                            "Tin nhắn mới từ shop",
                            toBodyPreview(message.getBody()),
                            buildChatMetadata(message)
                    ));
        }
    }

    @Transactional(readOnly = true)
    public ScrollResponse<RealtimeNotificationDTO> getUserNotifications(Long userId, Long cursor, int size) {
        int normalizedSize = normalizeSize(size);
        Long normalizedCursor = normalizeCursor(cursor);
        List<Notification> notifications = notificationRepository.findUserNotificationsForScroll(
                userId,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );
        return toScrollResponse(notifications, normalizedSize);
    }

    @Transactional(readOnly = true)
    public ScrollResponse<RealtimeNotificationDTO> getShopNotifications(
            Long shopId,
            Long currentUserId,
            Long cursor,
            int size
    ) {
        assertActiveShopMember(shopId, currentUserId);
        int normalizedSize = normalizeSize(size);
        Long normalizedCursor = normalizeCursor(cursor);
        List<Notification> notifications = notificationRepository.findShopNotificationsForScroll(
                shopId,
                normalizedCursor,
                PageRequest.of(0, normalizedSize + 1)
        );
        return toScrollResponse(notifications, normalizedSize);
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getUserUnreadCount(Long userId) {
        return new UnreadCountDTO(notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId));
    }

    @Transactional(readOnly = true)
    public UnreadCountDTO getShopUnreadCount(Long shopId, Long currentUserId) {
        assertActiveShopMember(shopId, currentUserId);
        return new UnreadCountDTO(notificationRepository.countByRecipientShopIdAndReadAtIsNull(shopId));
    }

    @Transactional
    public RealtimeNotificationDTO markRead(Long notificationId, Long currentUserId, Long shopId) {
        Notification notification = findNotification(notificationId);
        assertCanAccess(notification, currentUserId, shopId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toDTO(notification);
    }

    @Transactional
    public UnreadCountDTO markAllUserRead(Long userId) {
        notificationRepository.markAllUserRead(userId, OffsetDateTime.now());
        return getUserUnreadCount(userId);
    }

    @Transactional
    public UnreadCountDTO markAllShopRead(Long shopId, Long currentUserId) {
        assertActiveShopMember(shopId, currentUserId);
        notificationRepository.markAllShopRead(shopId, OffsetDateTime.now());
        return getShopUnreadCount(shopId, currentUserId);
    }

    private RealtimeNotificationDTO createAndPublish(
            NotificationRecipientType recipientType,
            Long recipientUserId,
            Long recipientShopId,
            Long shopId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            Long actorUserId,
            String title,
            String body,
            Map<String, Object> metadata
    ) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setRecipientType(recipientType);
        notification.setRecipientUserId(recipientUserId);
        notification.setRecipientShopId(recipientShopId);
        notification.setShopId(shopId);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setActorUserId(actorUserId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setMetadataJson(toMetadataJson(metadata));

        RealtimeNotificationDTO saved = toDTO(notificationRepository.save(notification));
        socketPublisher.publish(saved);
        return saved;
    }

    private ScrollResponse<RealtimeNotificationDTO> toScrollResponse(List<Notification> notifications, int size) {
        boolean hasNext = notifications.size() > size;
        List<Notification> content = notifications.stream()
                .limit(size)
                .toList();
        Long nextCursor = hasNext && !content.isEmpty()
                ? content.get(content.size() - 1).getId()
                : null;
        return ScrollResponse.of(content.stream().map(this::toDTO).toList(), size, nextCursor, hasNext);
    }

    private Notification findNotification(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFound(
                        "NotificationNotFound",
                        "Không tìm thấy thông báo"
                ));
    }

    private void assertCanAccess(Notification notification, Long currentUserId, Long shopId) {
        if (notification.getRecipientType() == NotificationRecipientType.USER) {
            if (!notification.getRecipientUserId().equals(currentUserId)) {
                throw new NotificationAccessDenied(
                        "NotificationAccessDenied",
                        "Không có quyền truy cập thông báo này"
                );
            }
            return;
        }

        if (notification.getRecipientType() == NotificationRecipientType.SHOP) {
            if (shopId == null || !notification.getRecipientShopId().equals(shopId)) {
                throw new NotificationAccessDenied(
                        "NotificationAccessDenied",
                        "Cần dùng shopId của thông báo"
                );
            }
            assertActiveShopMember(shopId, currentUserId);
        }
    }

    private void assertActiveShopMember(Long shopId, Long currentUserId) {
        if (currentUserId == null
                || !shopMemberRepository.existsByShopIdAndUserIdAndStatus(shopId, currentUserId, MemberStatus.ACTIVE)) {
            throw new NotificationAccessDenied(
                    "NotificationAccessDenied",
                    "Tài khoản không phải tài khoản đang hoạt động của shop"
            );
        }
    }

    private RealtimeNotificationDTO toDTO(Notification notification) {
        return new RealtimeNotificationDTO(
                notification.getId(),
                notification.getType(),
                notification.getRecipientType(),
                notification.getRecipientUserId(),
                notification.getRecipientShopId(),
                notification.getShopId(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getActorUserId(),
                notification.getTitle(),
                notification.getBody(),
                toResponseMetadata(notification),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private Map<String, Object> toResponseMetadata(Notification notification) {
        Map<String, Object> metadata = new LinkedHashMap<>(fromMetadataJson(notification.getMetadataJson()));
        if (notification.getType() == NotificationType.CHAT_MESSAGE
                && !metadata.containsKey("senderAvatarUrl")) {
            metadata.put("senderAvatarUrl", resolveSenderAvatarUrl(notification.getActorUserId()));
        }
        return metadata;
    }

    private String toMetadataJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception e) {
            throw new IllegalArgumentException("Metadata thông báo không hợp lệ");
        }
    }

    private Map<String, Object> fromMetadataJson(String metadataJson) {
        try {
            if (metadataJson == null || metadataJson.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> buildChatMetadata(MessageDTO message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("conversationId", message.getConversationId());
        metadata.put("messageId", message.getId());
        metadata.put("messageSenderType", message.getSenderType());
        metadata.put("senderUserId", message.getSenderUserId());
        metadata.put("senderAvatarUrl", resolveSenderAvatarUrl(message.getSenderUserId()));
        return metadata;
    }

    private String resolveSenderAvatarUrl(Long senderUserId) {
        if (senderUserId == null) {
            return null;
        }
        return userRepository.findById(senderUserId)
                .map(user -> user.getAvatarUrlPreview())
                .orElse(null);
    }

    private String toBodyPreview(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.trim();
        if (normalized.length() <= BODY_PREVIEW_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, BODY_PREVIEW_MAX_LENGTH);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SCROLL_SIZE);
    }

    private Long normalizeCursor(Long cursor) {
        return cursor != null && cursor > 0 ? cursor : null;
    }
}
