package com.orderly.api.shared.infrastructure.websocket;

import com.orderly.api.shared.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * STOMP over WebSocket configuration for real-time dashboard updates.
 *
 * Allowed origins mirror the HTTP CORS config (ORDERLY_CORS_ORIGINS env var) so
 * production domains are automatically included without hardcoding them here.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String[] allowedOrigins;

    public WebSocketConfig(CorsProperties corsProperties) {
        List<String> origins = corsProperties.allowedOrigins();
        // Always include the dev origin so local development works even when
        // ORDERLY_CORS_ORIGINS only lists the production domain.
        boolean hasDev = origins != null && origins.contains("http://localhost:4200");
        if (origins == null || origins.isEmpty()) {
            this.allowedOrigins = new String[]{"http://localhost:4200"};
        } else if (hasDev) {
            this.allowedOrigins = origins.toArray(new String[0]);
        } else {
            // Append dev origin to the production list
            String[] merged = new String[origins.size() + 1];
            origins.toArray(merged);
            merged[origins.size()] = "http://localhost:4200";
            this.allowedOrigins = merged;
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins).withSockJS();
    }
}
