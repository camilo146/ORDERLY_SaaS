package com.orderly.api.notification.application;

import com.orderly.api.business.infrastructure.persistence.BusinessJpaEntity;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.messaging.application.EvolutionApiService;
import com.orderly.api.notification.infrastructure.persistence.NotificationJpaEntity;
import com.orderly.api.notification.infrastructure.persistence.NotificationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Processes the notification_queue every 60 seconds.
 * Sends pending WhatsApp notifications via Evolution API.
 */
@Component
public class NotificationQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationQueueProcessor.class);
    private static final int MAX_RETRIES = 3;

    private final NotificationJpaRepository notificationRepo;
    private final BusinessJpaRepository businessRepo;
    private final EvolutionApiService evolutionApi;

    public NotificationQueueProcessor(
            NotificationJpaRepository notificationRepo,
            BusinessJpaRepository businessRepo,
            EvolutionApiService evolutionApi) {
        this.notificationRepo = notificationRepo;
        this.businessRepo = businessRepo;
        this.evolutionApi = evolutionApi;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processDueNotifications() {
        List<NotificationJpaEntity> due = notificationRepo.findDueNotifications(OffsetDateTime.now());
        if (due.isEmpty())
            return;

        log.info("Processing {} due notification(s)", due.size());

        for (NotificationJpaEntity notification : due) {
            try {
                sendNotification(notification);
                notification.setStatus("SENT");
                notification.setSentAt(OffsetDateTime.now());
            } catch (Exception e) {
                log.warn("Failed to send notification {}: {}", notification.getId(), e.getMessage());
                notification.setRetryCount(notification.getRetryCount() + 1);
                if (notification.getRetryCount() >= MAX_RETRIES) {
                    notification.setStatus("FAILED");
                    log.error("Notification {} permanently failed after {} retries",
                            notification.getId(), MAX_RETRIES);
                }
            }
            notificationRepo.save(notification);
        }
    }

    private void sendNotification(NotificationJpaEntity notification) {
        Optional<BusinessJpaEntity> businessOpt = businessRepo.findById(notification.getBusinessId());
        if (businessOpt.isEmpty()) {
            log.warn("Business not found for notification {}", notification.getId());
            return;
        }
        BusinessJpaEntity business = businessOpt.get();
        String instanceName = "orderly-" + business.getSlug();
        String recipientJid = notification.getRecipientPhone() + "@s.whatsapp.net";
        evolutionApi.sendTextMessage(instanceName, recipientJid, notification.getMessageBody());
    }
}
