package com.exe101.notification.controller;

import com.exe101.auth.model.UserPrincipal;
import com.exe101.common.ScrollResponse;
import com.exe101.notification.dto.RealtimeNotificationDTO;
import com.exe101.notification.dto.UnreadCountDTO;
import com.exe101.notification.exception.NotificationAccessDenied;
import com.exe101.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/users/me")
    public ResponseEntity<ScrollResponse<RealtimeNotificationDTO>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.getUserNotifications(
                getCurrentUserId(principal),
                cursor,
                size
        ));
    }

    @GetMapping("/users/me/unread-count")
    public ResponseEntity<UnreadCountDTO> getMyUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.getUserUnreadCount(getCurrentUserId(principal)));
    }

    @PatchMapping("/users/me/read-all")
    public ResponseEntity<UnreadCountDTO> markAllMyNotificationsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markAllUserRead(getCurrentUserId(principal)));
    }

    @GetMapping("/shops/{shopId}")
    public ResponseEntity<ScrollResponse<RealtimeNotificationDTO>> getShopNotifications(
            @PathVariable Long shopId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.getShopNotifications(
                shopId,
                getCurrentUserId(principal),
                cursor,
                size
        ));
    }

    @GetMapping("/shops/{shopId}/unread-count")
    public ResponseEntity<UnreadCountDTO> getShopUnreadCount(
            @PathVariable Long shopId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.getShopUnreadCount(shopId, getCurrentUserId(principal)));
    }

    @PatchMapping("/shops/{shopId}/read-all")
    public ResponseEntity<UnreadCountDTO> markAllShopNotificationsRead(
            @PathVariable Long shopId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markAllShopRead(shopId, getCurrentUserId(principal)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<RealtimeNotificationDTO> markNotificationRead(
            @PathVariable Long notificationId,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(notificationService.markRead(
                notificationId,
                getCurrentUserId(principal),
                shopId
        ));
    }

    private Long getCurrentUserId(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getId() == null) {
            throw new NotificationAccessDenied(
                    "AuthenticatedUserRequired",
                    "Cần đăng nhập để xem thông báo"
            );
        }
        return principal.getUser().getId();
    }
}
