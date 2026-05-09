package com.exe101.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeNotificationDTO {
    private Long id;
    private NotificationType type;
    private NotificationRecipientType recipientType;
    private Long recipientUserId;
    private Long recipientShopId;
    private Long shopId;
    private NotificationTargetType targetType;
    private Long targetId;
    private Long actorUserId;
    private String title;
    private String body;
    private Map<String, Object> metadata;
    private OffsetDateTime readAt;
    private OffsetDateTime createdAt;
}
