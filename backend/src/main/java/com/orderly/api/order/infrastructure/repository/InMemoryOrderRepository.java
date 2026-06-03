package com.orderly.api.order.infrastructure.repository;

import com.orderly.api.order.domain.model.Order;
import com.orderly.api.order.domain.port.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter for order persistence.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<UUID, Order> storage = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        storage.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findByIdAndBusinessId(UUID orderId, UUID businessId) {
        Order order = storage.get(orderId);
        if (order == null || !order.businessId().equals(businessId)) {
            return Optional.empty();
        }
        return Optional.of(order);
    }

    @Override
    public List<Order> findAllByBusinessId(UUID businessId) {
        return storage.values().stream()
                .filter(order -> order.businessId().equals(businessId))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    @Override
    public List<Order> findAll() {
        return storage.values().stream()
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    @Override
    public List<Order> findByCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status) {
        return storage.values().stream()
                .filter(o -> o.businessId().equals(businessId)
                        && o.customerWhatsapp().equals(customerWhatsapp)
                        && o.status().name().equals(status))
                .toList();
    }
}
