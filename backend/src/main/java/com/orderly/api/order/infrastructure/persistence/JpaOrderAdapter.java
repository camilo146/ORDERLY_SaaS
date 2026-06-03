package com.orderly.api.order.infrastructure.persistence;

import com.orderly.api.order.domain.model.Order;
import com.orderly.api.order.domain.model.OrderLine;
import com.orderly.api.order.domain.model.OrderStatus;
import com.orderly.api.order.domain.port.OrderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of OrderRepository.
 * Annotated @Primary so Spring injects this instead of the in-memory adapter.
 */
@Repository
@Primary
public class JpaOrderAdapter implements OrderRepository {

    private final OrderJpaRepository orderRepo;
    private final OrderItemJpaRepository itemRepo;

    public JpaOrderAdapter(OrderJpaRepository orderRepo, OrderItemJpaRepository itemRepo) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        orderRepo.save(toEntity(order));
        // Persist items — delete + re-insert is safe for immutable order items
        itemRepo.deleteAllByOrderId(order.id());
        List<OrderItemJpaEntity> items = order.items().stream()
                .map(line -> new OrderItemJpaEntity(
                        UUID.randomUUID(),
                        order.id(),
                        order.businessId(),
                        line.productId(),
                        line.productName(),
                        line.quantity(),
                        line.unitPrice(),
                        line.subtotal()))
                .toList();
        itemRepo.saveAll(items);
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByIdAndBusinessId(UUID orderId, UUID businessId) {
        return orderRepo.findByIdAndBusinessId(orderId, businessId)
                .map(e -> toDomain(e, itemRepo.findAllByOrderId(e.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByBusinessId(UUID businessId) {
        return orderRepo.findAllByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .map(e -> toDomain(e, itemRepo.findAllByOrderId(e.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepo.findAll().stream()
                .map(e -> toDomain(e, itemRepo.findAllByOrderId(e.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status) {
        return orderRepo.findAllByBusinessIdAndCustomerWhatsappAndStatus(businessId, customerWhatsapp, status)
                .stream().map(e -> toDomain(e, itemRepo.findAllByOrderId(e.getId()))).toList();
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private OrderJpaEntity toEntity(Order o) {
        Optional<OrderJpaEntity> existing = orderRepo.findById(o.id());
        if (existing.isPresent()) {
            existing.get().setStatus(o.status().name());
            existing.get().setPaymentProofUrl(o.paymentProofUrl());
            existing.get().setCancelReason(o.cancelReason());
            return existing.get();
        }
        return new OrderJpaEntity(o.id(), o.businessId(), o.customerName(), o.customerWhatsapp(),
                o.deliveryType(), o.notes(), o.totalAmount(), o.paymentMethod(),
                o.status().name(), o.createdAt());
    }

    private Order toDomain(OrderJpaEntity e, List<OrderItemJpaEntity> items) {
        List<OrderLine> lines = items.stream()
                .map(i -> new OrderLine(i.getProductId(), i.getProductNameSnapshot(),
                        i.getQuantity(), i.getUnitPrice(), i.getSubtotal(), null))
                .toList();
        return Order.reconstitute(e.getId(), e.getBusinessId(), e.getCustomerName(),
                e.getCustomerWhatsapp(), e.getDeliveryType(), e.getNotes(), lines,
                e.getTotalAmount(), OrderStatus.valueOf(e.getStatus()),
                e.getCreatedAt(), e.getPaymentMethod(), e.getPaymentProofUrl(), e.getCancelReason());
    }
}
