package com.orderly.api.product.application;

import com.orderly.api.product.domain.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Application boundary for product catalog operations.
 */
public interface ProductCatalogUseCase {

    Product create(CreateProductCommand command);

    List<Product> listByBusinessId(UUID businessId);

    Product findById(UUID businessId, UUID productId);

    Product uploadImage(UUID businessId, UUID productId, MultipartFile imageFile);

    Product update(UUID businessId, UUID productId, UpdateProductCommand command);

    void delete(UUID businessId, UUID productId);

    void decrementStock(UUID businessId, UUID productId, int quantity);
}
