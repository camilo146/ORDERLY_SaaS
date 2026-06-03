package com.orderly.api.shared.infrastructure.websocket;

import com.orderly.api.order.application.OrderChangedEvent;
import com.orderly.api.order.interfaces.rest.OrderResponse;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes order updates to tenant-specific WebSocket topics.
 */
@Component
public class OrderWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderWebSocketNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onOrderChanged(OrderChangedEvent event) {
        String destination = "/topic/business/" + event.order().businessId() + "/orders";
        Map<String, Object> payload = Map.of(
                "eventType", event.eventType(),
                "order", OrderResponse.from(event.order()));
        messagingTemplate.convertAndSend(destination, payload);
    }
}
