package com.orderly.api.plan.domain.port;

import com.orderly.api.plan.domain.model.SubscriptionUsage;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionUsageRepository {
    SubscriptionUsage save(SubscriptionUsage usage);
    Optional<SubscriptionUsage> findCurrentPeriod(UUID businessId, OffsetDateTime now);
    List<SubscriptionUsage> findAllByBusinessId(UUID businessId);
    List<SubscriptionUsage> findOpenPeriods();
}
