package com.exe101.notification.repository;

import com.exe101.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface INotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.recipientUserId = :userId
              AND notification.recipientType = com.exe101.notification.dto.NotificationRecipientType.USER
              AND (:cursor IS NULL OR notification.id < :cursor)
            ORDER BY notification.id DESC
            """)
    List<Notification> findUserNotificationsForScroll(
            @Param("userId") Long userId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            SELECT notification
            FROM Notification notification
            WHERE notification.recipientShopId = :shopId
              AND notification.recipientType = com.exe101.notification.dto.NotificationRecipientType.SHOP
              AND (:cursor IS NULL OR notification.id < :cursor)
            ORDER BY notification.id DESC
            """)
    List<Notification> findShopNotificationsForScroll(
            @Param("shopId") Long shopId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    long countByRecipientShopIdAndReadAtIsNull(Long recipientShopId);

    @Modifying
    @Query("""
            UPDATE Notification notification
            SET notification.readAt = :readAt
            WHERE notification.recipientUserId = :userId
              AND notification.recipientType = com.exe101.notification.dto.NotificationRecipientType.USER
              AND notification.readAt IS NULL
            """)
    int markAllUserRead(@Param("userId") Long userId, @Param("readAt") OffsetDateTime readAt);

    @Modifying
    @Query("""
            UPDATE Notification notification
            SET notification.readAt = :readAt
            WHERE notification.recipientShopId = :shopId
              AND notification.recipientType = com.exe101.notification.dto.NotificationRecipientType.SHOP
              AND notification.readAt IS NULL
            """)
    int markAllShopRead(@Param("shopId") Long shopId, @Param("readAt") OffsetDateTime readAt);
}
