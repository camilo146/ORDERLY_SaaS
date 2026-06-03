package com.orderly.api.product.infrastructure.persistence;

import com.orderly.api.product.domain.model.Product;
import com.orderly.api.product.domain.port.ProductRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA-backed implementation of ProductRepository.
 * Annotated @Primary so Spring injects this instead of the in-memory adapter.
 */
@Repository
@Primary
public class JpaProductAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepo;

    public JpaProductAdapter(ProductJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = toEntity(product);
        jpaRepo.save(entity);
        return product;
    }

    @Override
    public List<Product> findAllByBusinessId(UUID businessId) {
        return jpaRepo.findAllByBusinessIdOrderBySortOrderAsc(businessId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Product> findByIdAndBusinessId(UUID productId, UUID businessId) {
        return jpaRepo.findByIdAndBusinessId(productId, businessId).map(this::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(UUID productId, UUID businessId) {
        jpaRepo.findByIdAndBusinessId(productId, businessId)
                .ifPresent(e -> jpaRepo.deleteById(e.getId()));
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private ProductJpaEntity toEntity(Product p) {
        Optional<ProductJpaEntity> existing = jpaRepo.findById(p.id());
        if (existing.isPresent()) {
            ProductJpaEntity e = existing.get();
            e.setName(p.name());
            e.setDescription(p.description());
            e.setPrice(p.price());
            e.setImageUrl(p.imageUrl());
            e.setAvailable(p.available());
            e.setStock(p.stock());
            return e;
        }
        return new ProductJpaEntity(p.id(), p.businessId(), p.name(), p.description(),
                p.price(), p.available(), p.createdAt(), p.imageUrl());
    }

    private Product toDomain(ProductJpaEntity e) {
        return Product.reconstitute(e.getId(), e.getBusinessId(), e.getName(), e.getDescription(),
                e.getImageUrl(), e.getPrice(), e.isAvailable(), e.getCreatedAt(), e.getStock());
    }
}
