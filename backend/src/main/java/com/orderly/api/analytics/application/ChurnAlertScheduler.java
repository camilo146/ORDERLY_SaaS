package com.orderly.api.analytics.application;

import com.orderly.api.analytics.infrastructure.persistence.ChurnAlertJpaEntity;
import com.orderly.api.analytics.infrastructure.persistence.ChurnAlertJpaRepository;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaEntity;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.order.infrastructure.persistence.OrderJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Nightly churn alert scheduler.
 *
 * 4 jobs:
 * 02:00 AM — No orders in last 7 days (YELLOW)
 * 02:05 AM — No orders in last 30 days (RED)
 * 02:10 AM — No active subscription (YELLOW)
 * 02:15 AM — Business in TRIAL state (GREEN reminder)
 */
@Component
public class ChurnAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChurnAlertScheduler.class);

    private final ChurnAlertJpaRepository alertRepo;
    private final BusinessJpaRepository businessRepo;
    private final OrderJpaRepository orderRepo;

    public ChurnAlertScheduler(
            ChurnAlertJpaRepository alertRepo,
            BusinessJpaRepository businessRepo,
            OrderJpaRepository orderRepo) {
        this.alertRepo = alertRepo;
        this.businessRepo = businessRepo;
        this.orderRepo = orderRepo;
    }

    /** Check businesses with no orders in the last 7 days — YELLOW alert */
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Bogota")
    @Transactional
    public void checkNoOrdersLast7Days() {
        log.info("ChurnAlertScheduler: checking no-orders-7d");
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        List<BusinessJpaEntity> businesses = businessRepo.findAll();
        for (BusinessJpaEntity biz : businesses) {
            long count = orderRepo.countOrdersSince(biz.getId(), cutoff);
            if (count == 0 && !alertRepo.existsByBusinessIdAndAlertTypeAndResolvedFalse(
                    biz.getId(), "NO_ORDERS_7D")) {
                createAlert(biz.getId(), "YELLOW", "NO_ORDERS_7D",
                        "El negocio '" + biz.getName() + "' no ha recibido pedidos en 7 días.");
            }
        }
    }

    /** Check businesses with no orders in the last 30 days — RED alert */
    @Scheduled(cron = "0 5 2 * * *", zone = "America/Bogota")
    @Transactional
    public void checkNoOrdersLast30Days() {
        log.info("ChurnAlertScheduler: checking no-orders-30d");
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        List<BusinessJpaEntity> businesses = businessRepo.findAll();
        for (BusinessJpaEntity biz : businesses) {
            long count = orderRepo.countOrdersSince(biz.getId(), cutoff);
            if (count == 0 && !alertRepo.existsByBusinessIdAndAlertTypeAndResolvedFalse(
                    biz.getId(), "NO_ORDERS_30D")) {
                createAlert(biz.getId(), "RED", "NO_ORDERS_30D",
                        "El negocio '" + biz.getName() + "' no ha recibido pedidos en 30 días. " +
                                "Alto riesgo de churn.");
            }
        }
    }

    /**
     * Check businesses in PENDING status > 48 hours — they may need activation help
     */
    @Scheduled(cron = "0 10 2 * * *", zone = "America/Bogota")
    @Transactional
    public void checkStuckOnboarding() {
        log.info("ChurnAlertScheduler: checking stuck onboarding");
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(48);
        List<BusinessJpaEntity> stuck = businessRepo.findAll().stream()
                .filter(b -> "PENDING".equals(b.getStatus()) && b.getCreatedAt().isBefore(cutoff))
                .toList();
        for (BusinessJpaEntity biz : stuck) {
            if (!alertRepo.existsByBusinessIdAndAlertTypeAndResolvedFalse(biz.getId(), "STUCK_ONBOARDING")) {
                createAlert(biz.getId(), "YELLOW", "STUCK_ONBOARDING",
                        "El negocio '" + biz.getName() + "' lleva más de 48h en estado PENDING " +
                                "(onboarding incompleto).");
            }
        }
    }

    /** Check businesses in TRIAL — remind to convert before expiry */
    @Scheduled(cron = "0 15 2 * * *", zone = "America/Bogota")
    @Transactional
    public void checkTrialExpiry() {
        log.info("ChurnAlertScheduler: checking trial businesses");
        List<BusinessJpaEntity> trials = businessRepo.findAll().stream()
                .filter(b -> "TRIAL".equals(b.getStatus()))
                .toList();
        for (BusinessJpaEntity biz : trials) {
            if (!alertRepo.existsByBusinessIdAndAlertTypeAndResolvedFalse(biz.getId(), "TRIAL_REMINDER")) {
                createAlert(biz.getId(), "GREEN", "TRIAL_REMINDER",
                        "El negocio '" + biz.getName() + "' está en periodo de prueba. " +
                                "Recuérdale actualizar a un plan pago.");
            }
        }
    }

    private void createAlert(UUID businessId, String severity, String alertType, String description) {
        ChurnAlertJpaEntity alert = new ChurnAlertJpaEntity(
                UUID.randomUUID(), businessId, severity, alertType, description);
        alertRepo.save(alert);
        log.info("ChurnAlert created: business={} severity={} type={}", businessId, severity, alertType);
    }
}
