package com.orderly.api.product.infrastructure.repository;

import com.orderly.api.product.domain.model.Product;
import com.orderly.api.product.domain.port.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory adapter for product catalog persistence.
 */
@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<UUID, Product> storage = new ConcurrentHashMap<>();

    @Override
    public Product save(Product product) {
        storage.put(product.id(), product);
        return product;
    }

    @Override
    public List<Product> findAllByBusinessId(UUID businessId) {
        return storage.values().stream()
                .filter(product -> product.businessId().equals(businessId))
                .sorted((left, right) -> left.name().compareToIgnoreCase(right.name()))
                .toList();
    }

    @Override
    public Optional<Product> findByIdAndBusinessId(UUID productId, UUID businessId) {
        Product product = storage.get(productId);
        if (product == null || !product.businessId().equals(businessId)) {
            return Optional.empty();
        }
        return Optional.of(product);
    }

    @Override
    public List<Product> findAll() {
        return storage.values().stream().toList();
    }

    @Override
    public void delete(UUID productId, UUID businessId) {
        storage.computeIfPresent(productId, (id, p) -> p.businessId().equals(businessId) ? null : p);
    }
}
