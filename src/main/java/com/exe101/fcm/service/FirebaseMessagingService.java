package com.exe101.fcm.service;

import com.exe101.fcm.entity.FcmToken;
import com.exe101.fcm.repository.IFcmTokenRepository;
import com.exe101.user.entity.User;
import com.exe101.user.repository.IUserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseMessagingService {

    private final IFcmTokenRepository fcmTokenRepository;
    private final IUserRepository userRepository;

    @Transactional
    public void saveToken(Long userId, String token) {
        if (!fcmTokenRepository.existsByUserIdAndToken(userId, token)) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
                    
            FcmToken fcmToken = FcmToken.builder()
                    .user(user)
                    .token(token)
                    .build();
            fcmTokenRepository.save(fcmToken);
            log.info("Saved FCM token for user {}", userId);
        }
    }

    public void sendPushNotification(Long userId, String title, String body, Map<String, String> data) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        
        if (tokens.isEmpty()) {
            log.debug("No FCM token found for user {}", userId);
            return;
        }

        try {
            FirebaseMessaging messaging = FirebaseMessaging.getInstance();
            
            for (FcmToken fcmToken : tokens) {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());
                                
                if (data != null && !data.isEmpty()) {
                    messageBuilder.putAllData(data);
                }

                String response = messaging.send(messageBuilder.build());
                log.info("Successfully sent message to token {}: {}", fcmToken.getToken(), response);
            }
        } catch (Exception e) {
            log.error("Failed to send Firebase push notification to user {}", userId, e);
        }
    }
}
