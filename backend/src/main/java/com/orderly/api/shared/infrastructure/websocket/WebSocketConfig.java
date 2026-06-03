package com.orderly.api.shared.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration for real-time dashboard updates.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // [SECURITY FIX VUL-02] Restringir origen del WebSocket a dominios conocidos.
        // "allowedOriginPatterns(*)" permitía que cualquier sitio web externo se
        // conectara y recibiera notificaciones de órdenes en tiempo real (data breach).
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200", "http://10.*.*.*:4200");
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200", "http://10.*.*.*:4200")
                .withSockJS();
    }
}
