package com.orderly.api.email.scheduler;

import com.orderly.api.business.infrastructure.persistence.BusinessJpaEntity;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.email.application.UserEmailService;
import com.orderly.api.shared.security.persistence.UserJpaEntity;
import com.orderly.api.shared.security.persistence.UserJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Component
public class EmailReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailReminderScheduler.class);

    private final UserEmailService userEmailService;
    private final UserJpaRepository userRepo;
    private final BusinessJpaRepository businessRepo;

    public EmailReminderScheduler(
            UserEmailService userEmailService,
            UserJpaRepository userRepo,
            BusinessJpaRepository businessRepo) {
        this.userEmailService = userEmailService;
        this.userRepo = userRepo;
        this.businessRepo = businessRepo;
    }

    /** 09:00 AM — Recordatorio onboarding: negocios en PENDING por más de 48h */
    @Scheduled(cron = "0 0 9 * * *", zone = "America/Bogota")
    @Transactional
    public void sendOnboardingReminders() {
        log.info("EmailReminderScheduler: onboarding reminders");
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusHours(48);
        List<BusinessJpaEntity> stuck = businessRepo.findAll().stream()
                .filter(b -> "PENDING".equals(b.getStatus()) && b.getCreatedAt().isBefore(cutoff))
                .toList();

        for (BusinessJpaEntity biz : stuck) {
            Optional<UserJpaEntity> ownerOpt = userRepo.findById(biz.getOwnerId());
            if (ownerOpt.isEmpty()) continue;
            UserJpaEntity owner = ownerOpt.get();

            if (owner.getOnboardingEmailSentAt() != null &&
                owner.getOnboardingEmailSentAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC).minusDays(3))) {
                continue; // No re-enviar más de una vez cada 3 días
            }

            try {
                int step = biz.getOnboardingStep() != null ? biz.getOnboardingStep() : 0;
                userEmailService.sendOnboardingReminderEmail(
                        owner.getEmail(), owner.getFullName(), biz.getName(), step);
                owner.setOnboardingEmailSentAt(OffsetDateTime.now(ZoneOffset.UTC));
                userRepo.save(owner);
            } catch (Exception e) {
                log.warn("Failed onboarding reminder for business {}: {}", biz.getId(), e.getMessage());
            }
        }
    }

    /** 09:10 AM — Re-engagement: usuarios sin login en 30 días */
    @Scheduled(cron = "0 10 9 * * *", zone = "America/Bogota")
    @Transactional
    public void sendInactivityReminders() {
        log.info("EmailReminderScheduler: inactivity reminders");
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        List<UserJpaEntity> inactive = userRepo.findInactiveUsersWithNoReminderSince(cutoff);

        for (UserJpaEntity user : inactive) {
            List<BusinessJpaEntity> businesses = businessRepo.findAll().stream()
                    .filter(b -> b.getOwnerId().equals(user.getId()))
                    .toList();
            if (businesses.isEmpty()) continue;

            try {
                String businessName = businesses.get(0).getName();
                userEmailService.sendInactivityReminderEmail(user.getEmail(), user.getFullName(), businessName);
                user.setInactivityEmailSentAt(OffsetDateTime.now(ZoneOffset.UTC));
                userRepo.save(user);
            } catch (Exception e) {
                log.warn("Failed inactivity reminder for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /** 09:20 AM — Trial expiry: negocios en TRIAL, recordatorio de conversión */
    @Scheduled(cron = "0 20 9 * * *", zone = "America/Bogota")
    @Transactional
    public void sendTrialExpiryReminders() {
        log.info("EmailReminderScheduler: trial expiry reminders");
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        List<BusinessJpaEntity> trials = businessRepo.findAll().stream()
                .filter(b -> "TRIAL".equals(b.getStatus()) && b.getCreatedAt().isBefore(sevenDaysAgo))
                .toList();

        for (BusinessJpaEntity biz : trials) {
            Optional<UserJpaEntity> ownerOpt = userRepo.findById(biz.getOwnerId());
            if (ownerOpt.isEmpty()) continue;
            UserJpaEntity owner = ownerOpt.get();

            try {
                long daysInTrial = java.time.Duration.between(biz.getCreatedAt(), OffsetDateTime.now(ZoneOffset.UTC)).toDays();
                int trialDays = 14;
                int daysLeft = Math.max(0, trialDays - (int) daysInTrial);
                userEmailService.sendTrialExpiryEmail(owner.getEmail(), owner.getFullName(), biz.getName(), daysLeft);
            } catch (Exception e) {
                log.warn("Failed trial expiry reminder for business {}: {}", biz.getId(), e.getMessage());
            }
        }
    }
}
