package com.orderly.api.product.interfaces.rest;

import com.orderly.api.product.application.CreateProductCommand;
import com.orderly.api.product.application.ProductCatalogUseCase;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.orderly.api.product.application.UpdateProductCommand;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for tenant product catalog management.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/products")
public class ProductController {

    private final ProductCatalogUseCase productCatalogUseCase;
    private final TenantAccessService tenantAccessService;

    public ProductController(ProductCatalogUseCase productCatalogUseCase, TenantAccessService tenantAccessService) {
        this.productCatalogUseCase = productCatalogUseCase;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@PathVariable UUID businessId, @Valid @RequestBody CreateProductRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        CreateProductCommand command = new CreateProductCommand(tenantId, request.name(), request.description(),
                request.imageUrl(), request.price());
        return ProductResponse.from(productCatalogUseCase.create(command));
    }

    @PostMapping(value = "/{productId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse uploadImage(
            @PathVariable UUID businessId,
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return ProductResponse.from(productCatalogUseCase.uploadImage(tenantId, productId, file));
    }

    @GetMapping
    public List<ProductResponse> list(@PathVariable UUID businessId) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return productCatalogUseCase.listByBusinessId(tenantId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PutMapping("/{productId}")
    public ProductResponse update(
            @PathVariable UUID businessId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        UpdateProductCommand command = new UpdateProductCommand(
                request.name(), request.description(), request.price(), request.available(), request.stock());
        return ProductResponse.from(productCatalogUseCase.update(tenantId, productId, command));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID businessId, @PathVariable UUID productId) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        productCatalogUseCase.delete(tenantId, productId);
    }
}
