package com.orderly.api.dashboard.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Summary metrics for the tenant admin panel.
 */
public record BusinessDashboardResponse(
        String role,
        UUID businessId,
        String businessName,
        int totalProducts,
        int totalOrders,
        int pendingOrders,
        int inProgressOrders,
        int readyOrders,
        int deliveredOrders,
        BigDecimal totalRevenue) {
}
