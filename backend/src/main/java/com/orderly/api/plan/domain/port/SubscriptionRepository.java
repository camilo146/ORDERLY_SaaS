package com.orderly.api.plan.domain.port;

import com.orderly.api.plan.domain.model.Subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findByBusinessId(UUID businessId);
    Optional<Subscription> findById(UUID id);
    Optional<Subscription> findByExternalSubscriptionId(String externalSubscriptionId);
}
