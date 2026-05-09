package com.exe101.notification.service;

import com.exe101.notification.dto.NotificationRecipientType;
import com.exe101.notification.dto.RealtimeNotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(RealtimeNotificationDTO notification) {
        if (notification.getRecipientType() == NotificationRecipientType.SHOP
                && notification.getRecipientShopId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/shops/" + notification.getRecipientShopId() + "/notifications",
                    notification
            );
            return;
        }

        if (notification.getRecipientType() == NotificationRecipientType.USER
                && notification.getRecipientUserId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/users/" + notification.getRecipientUserId() + "/notifications",
                    notification
            );
        }
    }
}
