package com.orderly.api.order.application;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.messaging.application.EvolutionApiService;
import com.orderly.api.order.domain.model.Order;
import com.orderly.api.order.domain.model.OrderLine;
import com.orderly.api.order.domain.model.OrderStatus;
import com.orderly.api.order.domain.port.OrderRepository;
import com.orderly.api.plan.application.PlanLimitService;
import com.orderly.api.plan.application.UsageTrackingService;
import com.orderly.api.product.application.ProductCatalogUseCase;
import com.orderly.api.product.domain.model.Product;
import com.orderly.api.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Coordinates order intake, pricing and lifecycle updates.
 */
@Service
public class DefaultOrderManagementService implements OrderManagementUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultOrderManagementService.class);

    private final OrderRepository orderRepository;
    private final ProductCatalogUseCase productCatalogUseCase;
    private final PlanLimitService planLimitService;
    private final UsageTrackingService usageTrackingService;
    private final ApplicationEventPublisher eventPublisher;
    private final EvolutionApiService evolutionApi;
    private final BusinessRepository businessRepository;

    public DefaultOrderManagementService(
            OrderRepository orderRepository,
            ProductCatalogUseCase productCatalogUseCase,
            PlanLimitService planLimitService,
            UsageTrackingService usageTrackingService,
            ApplicationEventPublisher eventPublisher,
            EvolutionApiService evolutionApi,
            BusinessRepository businessRepository) {
        this.orderRepository = orderRepository;
        this.productCatalogUseCase = productCatalogUseCase;
        this.planLimitService = planLimitService;
        this.usageTrackingService = usageTrackingService;
        this.eventPublisher = eventPublisher;
        this.evolutionApi = evolutionApi;
        this.businessRepository = businessRepository;
    }

    @Override
    public Order create(CreateOrderCommand command) {
        planLimitService.assertCanCreateOrder(command.businessId());

        String paymentMethod = command.paymentMethod() == null || command.paymentMethod().isBlank()
                ? "CASH"
                : command.paymentMethod().trim().toUpperCase();

        List<OrderLine> lines = command.items().stream()
                .map(item -> toLine(command.businessId(), item))
                .toList();

        BigDecimal totalAmount = lines.stream()
                .map(OrderLine::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order savedOrder = orderRepository.save(Order.create(
                command.businessId(),
                command.customerName(),
                command.customerWhatsapp(),
                command.deliveryType(),
                command.notes(),
                lines,
                totalAmount,
                paymentMethod));

        eventPublisher.publishEvent(new OrderChangedEvent("order.created", savedOrder));

        // Soft monthly limit: record usage and compute overage (never blocks the order)
        try {
            usageTrackingService.recordOrderCreated(command.businessId());
        } catch (Exception e) {
            log.warn("Usage tracking failed for business {}: {}", command.businessId(), e.getMessage());
        }

        // Decrement stock for each ordered item
        for (CreateOrderCommand.OrderItemInput item : command.items()) {
            try {
                productCatalogUseCase.decrementStock(command.businessId(), item.productId(), item.quantity());
            } catch (Exception e) {
                log.warn("Could not decrement stock for product {}: {}", item.productId(), e.getMessage());
            }
        }

        return savedOrder;
    }

    @Override
    public List<Order> listByBusinessId(UUID businessId) {
        return orderRepository.findAllByBusinessId(businessId);
    }

    @Override
    public Order updateStatus(UUID businessId, UUID orderId, String newStatus) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new DomainException("Order not found: " + orderId));

        Order updatedOrder = orderRepository
                .save(order.updateStatus(OrderStatus.valueOf(newStatus.trim().toUpperCase())));
        eventPublisher.publishEvent(new OrderChangedEvent("order.updated", updatedOrder));
        return updatedOrder;
    }

    @Override
    public Order setPaymentProof(UUID businessId, UUID orderId, String proofUrl) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new DomainException("Order not found: " + orderId));
        Order updated = orderRepository.save(order.withPaymentProof(proofUrl));
        eventPublisher.publishEvent(new OrderChangedEvent("order.updated", updated));
        return updated;
    }

    @Override
    public Order cancelWithReason(UUID businessId, UUID orderId, String reason) {
        Order order = orderRepository.findByIdAndBusinessId(orderId, businessId)
                .orElseThrow(() -> new DomainException("Order not found: " + orderId));
        Order cancelled = orderRepository.save(order.withCancelReason(reason));
        eventPublisher.publishEvent(new OrderChangedEvent("order.cancelled", cancelled));
        sendCancellationMessage(cancelled, reason);
        return cancelled;
    }

    @Override
    public List<Order> findByCustomerWhatsappAndStatus(UUID businessId, String customerWhatsapp, String status) {
        return orderRepository.findByCustomerWhatsappAndStatus(businessId, customerWhatsapp, status);
    }

    private void sendCancellationMessage(Order order, String reason) {
        try {
            businessRepository.findById(order.businessId()).ifPresent(business -> {
                String instance = "orderly-" + business.slug();
                String jid = order.customerWhatsapp() + "@s.whatsapp.net";
                String msg = "❌ *Tu pedido ha sido cancelado.*\n\n"
                        + (reason != null && !reason.isBlank()
                                ? "Motivo: _" + reason + "_\n\n"
                                : "")
                        + "Si tienes dudas, contáctanos. Escribe *0* para hacer un nuevo pedido.";
                evolutionApi.sendTextMessage(instance, jid, msg);
            });
        } catch (Exception e) {
            log.warn("Could not send cancellation WA message for order {}: {}", order.id(), e.getMessage());
        }
    }

    private OrderLine toLine(UUID businessId, CreateOrderCommand.OrderItemInput item) {
        Product product = productCatalogUseCase.findById(businessId, item.productId());
        if (item.quantity() <= 0) {
            throw new DomainException("Item quantity must be positive.");
        }

        BigDecimal subtotal = product.price().multiply(BigDecimal.valueOf(item.quantity()));
        return new OrderLine(item.productId(), product.name(), item.quantity(), product.price(), subtotal,
                item.notes());
    }
}
