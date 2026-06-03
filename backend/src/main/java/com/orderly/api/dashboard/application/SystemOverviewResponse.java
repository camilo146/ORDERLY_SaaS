package com.orderly.api.dashboard.application;

import java.math.BigDecimal;

/**
 * Summary metrics for the super admin panel.
 */
public record SystemOverviewResponse(
        String role,
        int totalBusinesses,
        int totalProducts,
        int totalOrders,
        int pendingOrders,
        int readyOrders,
        BigDecimal totalRevenue) {
}
