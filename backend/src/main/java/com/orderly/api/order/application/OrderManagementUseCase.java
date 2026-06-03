package com.orderly.api.order.application;

import com.orderly.api.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

/**
 * Application boundary for order intake and lifecycle changes.
 */
public interface OrderManagementUseCase {

    Order create(CreateOrderCommand command);

    List<Order> listByBusinessId(UUID businessId);

    Order updateStatus(UUID businessId, UUID orderId, String newStatus);

    Order setPaymentProof(UUID businessId, UUID orderId, String proofUrl);

    Order cancelWithReason(UUID businessId, UUID orderId, String reason);

    List<Order> findByCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status);
}
