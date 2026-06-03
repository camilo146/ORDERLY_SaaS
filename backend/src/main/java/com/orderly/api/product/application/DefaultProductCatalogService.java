package com.orderly.api.product.application;

import com.orderly.api.product.domain.model.Product;
import com.orderly.api.product.domain.port.ProductRepository;
import com.orderly.api.product.infrastructure.storage.ProductImageStorageService;
import com.orderly.api.shared.domain.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates product catalog use cases.
 */
@Service
public class DefaultProductCatalogService implements ProductCatalogUseCase {

    private final ProductRepository productRepository;
    private final ProductImageStorageService productImageStorageService;

    public DefaultProductCatalogService(
            ProductRepository productRepository,
            ProductImageStorageService productImageStorageService) {
        this.productRepository = productRepository;
        this.productImageStorageService = productImageStorageService;
    }

    @Override
    public Product create(CreateProductCommand command) {
        Product product = Product.create(
                command.businessId(),
                command.name(),
                command.description(),
                command.imageUrl(),
                command.price());
        return productRepository.save(product);
    }

    @Override
    public List<Product> listByBusinessId(UUID businessId) {
        return productRepository.findAllByBusinessId(businessId);
    }

    @Override
    public Product findById(UUID businessId, UUID productId) {
        return productRepository.findByIdAndBusinessId(productId, businessId)
                .orElseThrow(() -> new DomainException("Product not found: " + productId));
    }

    @Override
    public Product uploadImage(UUID businessId, UUID productId, MultipartFile imageFile) {
        Product existing = findById(businessId, productId);
        String imageUrl = productImageStorageService.store(businessId, productId, imageFile);
        return productRepository.save(existing.withImageUrl(imageUrl));
    }

    @Override
    public Product update(UUID businessId, UUID productId, UpdateProductCommand command) {
        Product existing = findById(businessId, productId);
        Product updated = existing.withUpdatedFields(command.name(), command.description(), command.price(), command.available(), command.stock());
        return productRepository.save(updated);
    }

    @Override
    public void delete(UUID businessId, UUID productId) {
        findById(businessId, productId);
        productRepository.delete(productId, businessId);
    }

    @Override
    public void decrementStock(UUID businessId, UUID productId, int quantity) {
        Product existing = findById(businessId, productId);
        if (existing.stock() != null) {
            productRepository.save(existing.decrementStock(quantity));
        }
    }
}
