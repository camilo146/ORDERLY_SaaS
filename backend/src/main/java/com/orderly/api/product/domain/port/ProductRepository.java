package com.orderly.api.product.domain.port;

import com.orderly.api.product.domain.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for product catalog operations.
 */
public interface ProductRepository {

    Product save(Product product);

    List<Product> findAllByBusinessId(UUID businessId);

    Optional<Product> findByIdAndBusinessId(UUID productId, UUID businessId);

    List<Product> findAll();

    void delete(UUID productId, UUID businessId);
}
