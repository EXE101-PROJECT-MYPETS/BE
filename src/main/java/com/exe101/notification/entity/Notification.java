package com.exe101.notification.entity;

import com.exe101.notification.dto.NotificationRecipientType;
import com.exe101.notification.dto.NotificationTargetType;
import com.exe101.notification.dto.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 32)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Column(name = "recipient_shop_id")
    private Long recipientShopId;

    @Column(name = "shop_id")
    private Long shopId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 64)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
