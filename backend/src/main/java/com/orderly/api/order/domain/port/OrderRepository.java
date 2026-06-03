package com.orderly.api.order.domain.port;

import com.orderly.api.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for tenant orders.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findByIdAndBusinessId(UUID orderId, UUID businessId);

    List<Order> findAllByBusinessId(UUID businessId);

    List<Order> findAll();

    List<Order> findByCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status);
}
