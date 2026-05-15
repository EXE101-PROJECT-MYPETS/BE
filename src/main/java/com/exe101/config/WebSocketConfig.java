package com.exe101.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String[] ALLOWED_ORIGIN_PATTERNS = {
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://192.168.*.*:*",
            "http://10.*.*.*:*",
            "http://localhost:3000",
            "http://localhost:5173",
            "https://exe-fe-gold.vercel.app",
            "https://*.vercel.app",
            "https://*.ngrok-free.dev"
    };

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setTaskScheduler(webSocketMessageBrokerTaskScheduler())
                .setHeartbeatValue(new long[]{10000, 10000});
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);

        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
                .withSockJS();
    }

    @Bean
    public TaskScheduler webSocketMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
