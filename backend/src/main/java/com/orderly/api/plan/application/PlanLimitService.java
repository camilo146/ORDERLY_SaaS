package com.orderly.api.plan.application;

import com.orderly.api.order.infrastructure.persistence.OrderJpaRepository;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaEntity;
import com.orderly.api.plan.infrastructure.persistence.PlanJpaRepository;
import com.orderly.api.shared.domain.PlanLimitExceededException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Validates plan-based limits before allowing certain operations.
 */
@Service
public class PlanLimitService {

    private final PlanJpaRepository planRepo;
    private final OrderJpaRepository orderRepo;

    public PlanLimitService(PlanJpaRepository planRepo, OrderJpaRepository orderRepo) {
        this.planRepo = planRepo;
        this.orderRepo = orderRepo;
    }

    /**
     * Checks whether a new order can be created for the given business.
     * Throws PlanLimitExceededException if the active order limit has been reached.
     */
    public void assertCanCreateOrder(UUID businessId) {
        Optional<PlanJpaEntity> planOpt = planRepo.findByBusinessId(businessId);
        if (planOpt.isEmpty())
            return; // No plan assigned — no restriction

        PlanJpaEntity plan = planOpt.get();
        Integer maxActiveOrders = plan.getMaxActiveOrders();
        if (maxActiveOrders == null)
            return; // null = unlimited (PREMIUM)

        long currentActiveOrders = orderRepo.countActiveOrders(businessId);
        if (currentActiveOrders >= maxActiveOrders) {
            throw new PlanLimitExceededException(String.format(
                    "Plan '%s' allows a maximum of %d active orders. Currently at %d. " +
                            "Complete or cancel existing orders, or upgrade your plan.",
                    plan.getName(), maxActiveOrders, currentActiveOrders));
        }
    }
}
