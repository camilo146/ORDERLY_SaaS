package com.orderly.api.dashboard.application;

import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.order.domain.model.Order;
import com.orderly.api.order.domain.model.OrderStatus;
import com.orderly.api.order.domain.port.OrderRepository;
import com.orderly.api.product.domain.port.ProductRepository;
import com.orderly.api.shared.domain.DomainException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Provides aggregated dashboard data for super admin, tenant admins and
 * operators.
 */
@Service
public class DashboardQueryService {

    private final BusinessRepository businessRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public DashboardQueryService(
            BusinessRepository businessRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        this.businessRepository = businessRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public SystemOverviewResponse getSystemOverview() {
        List<Order> allOrders = orderRepository.findAll();
        return new SystemOverviewResponse(
                "SUPER_ADMIN",
                businessRepository.findAll().size(),
                productRepository.findAll().size(),
                allOrders.size(),
                countByStatus(allOrders, OrderStatus.PENDING),
                countByStatus(allOrders, OrderStatus.READY),
                totalRevenue(allOrders));
    }

    public BusinessDashboardResponse getBusinessOverview(UUID businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new DomainException("Business not found: " + businessId));

        List<Order> orders = orderRepository.findAllByBusinessId(businessId);

        return new BusinessDashboardResponse(
                "ADMIN",
                business.id(),
                business.name(),
                productRepository.findAllByBusinessId(businessId).size(),
                orders.size(),
                countByStatus(orders, OrderStatus.PENDING),
                countByStatus(orders, OrderStatus.IN_PROGRESS),
                countByStatus(orders, OrderStatus.READY),
                countByStatus(orders, OrderStatus.DELIVERED),
                totalRevenue(orders));
    }

    public OperatorOverviewResponse getOperatorOverview() {
        List<Order> allOrders = orderRepository.findAll();
        return new OperatorOverviewResponse(
                "OPERATOR",
                businessRepository.findAll().size(),
                countByStatus(allOrders, OrderStatus.PENDING),
                countByStatus(allOrders, OrderStatus.IN_PROGRESS),
                countByStatus(allOrders, OrderStatus.READY),
                0);
    }

    private int countByStatus(List<Order> orders, OrderStatus status) {
        return (int) orders.stream().filter(order -> order.status() == status).count();
    }

    private BigDecimal totalRevenue(List<Order> orders) {
        return orders.stream()
                .map(Order::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
