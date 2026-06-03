package com.orderly.api.dashboard.application;

/**
 * Summary metrics for the operations panel.
 */
public record OperatorOverviewResponse(
        String role,
        int visibleBusinesses,
        int pendingOrders,
        int inProgressOrders,
        int readyOrders,
        int openHandoffs) {
}
