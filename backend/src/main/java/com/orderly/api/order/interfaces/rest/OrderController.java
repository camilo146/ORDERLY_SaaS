package com.orderly.api.order.interfaces.rest;

import com.orderly.api.order.application.CreateOrderCommand;
import com.orderly.api.order.application.OrderManagementUseCase;
import com.orderly.api.shared.security.UrlSafetyValidator;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for tenant order management.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/orders")
public class OrderController {

    private final OrderManagementUseCase orderManagementUseCase;
    private final TenantAccessService tenantAccessService;

    public OrderController(OrderManagementUseCase orderManagementUseCase, TenantAccessService tenantAccessService) {
        this.orderManagementUseCase = orderManagementUseCase;
        this.tenantAccessService = tenantAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@PathVariable UUID businessId, @Valid @RequestBody CreateOrderRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        CreateOrderCommand command = new CreateOrderCommand(
                tenantId,
                request.customerName(),
                request.customerWhatsapp(),
                request.deliveryType(),
                request.notes(),
                request.items().stream().map(
                        item -> new CreateOrderCommand.OrderItemInput(item.productId(), item.quantity(), item.notes()))
                        .toList(),
                request.paymentMethod());
        return OrderResponse.from(orderManagementUseCase.create(command));
    }

    @GetMapping
    public List<OrderResponse> list(@PathVariable UUID businessId) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return orderManagementUseCase.listByBusinessId(tenantId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable UUID businessId,
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return OrderResponse.from(orderManagementUseCase.updateStatus(tenantId, orderId, request.status()));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelWithReason(
            @PathVariable UUID businessId,
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        return OrderResponse.from(orderManagementUseCase.cancelWithReason(tenantId, orderId, request.reason()));
    }

    @PatchMapping("/{orderId}/payment-proof")
    public OrderResponse setPaymentProof(
            @PathVariable UUID businessId,
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentProofRequest request) {
        UUID tenantId = tenantAccessService.validateTenantAccess(businessId);
        // Validate the URL is a safe public HTTPS endpoint before persisting and passing
        // it to Evolution API (which makes an outbound HTTP request to fetch the image).
        // This prevents SSRF attacks targeting internal services or cloud metadata endpoints.
        String safeUrl = UrlSafetyValidator.validatePublicUrl(request.proofUrl(), "proofUrl");
        return OrderResponse.from(orderManagementUseCase.setPaymentProof(tenantId, orderId, safeUrl));
    }

    // ── Request records ───────────────────────────────────────────────────────

    /**
     * @param proofUrl publicly reachable HTTPS URL of the payment screenshot.
     *                 Max 2000 chars (RFC 7230 recommendation for URL length limits).
     *                 UrlSafetyValidator blocks private IPs and cloud metadata endpoints.
     */
    public record PaymentProofRequest(
            @Size(max = 2000) String proofUrl) {
    }

    /**
     * @param reason human-readable cancellation reason; capped at 500 chars to prevent
     *               oversized payloads being stored in the database.
     */
    public record CancelOrderRequest(
            @Size(max = 500) String reason) {
    }
}
