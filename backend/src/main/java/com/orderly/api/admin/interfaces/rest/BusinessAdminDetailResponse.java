package com.orderly.api.admin.interfaces.rest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BusinessAdminDetailResponse(
        UUID id,
        String name,
        String slug,
        String businessType,
        String status,
        String countryCode,
        String currencyCode,
        String timezone,
        OffsetDateTime createdAt,
        UUID ownerId,
        String ownerEmail,
        String ownerName,
        PlanInfo plan,
        SubscriptionInfo subscription,
        UsageInfo usage,
        int totalOrders,
        BigDecimal totalRevenue
) {
    public record PlanInfo(
            UUID id,
            String code,
            String name,
            BigDecimal monthlyPrice,
            BigDecimal monthlyPriceUsd
    ) {}

    public record SubscriptionInfo(
            String status,
            OffsetDateTime trialEndsAt,
            OffsetDateTime currentPeriodEnd,
            String provider
    ) {}

    public record UsageInfo(
            int ordersThisMonth,
            int orderLimit,
            int overageCount
    ) {}
}
