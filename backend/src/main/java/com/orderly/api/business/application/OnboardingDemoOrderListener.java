package com.orderly.api.business.application;

import com.orderly.api.business.domain.event.BusinessWhatsAppConnectedEvent;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaEntity;
import com.orderly.api.business.infrastructure.persistence.BusinessJpaRepository;
import com.orderly.api.messaging.application.EvolutionApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Sends a demo order message 30 seconds after WhatsApp first connects.
 * Anti-churn onboarding hook.
 */
@Component
public class OnboardingDemoOrderListener {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDemoOrderListener.class);

    private final BusinessJpaRepository businessJpaRepository;
    private final EvolutionApiService evolutionApi;

    public OnboardingDemoOrderListener(
            BusinessJpaRepository businessJpaRepository,
            EvolutionApiService evolutionApi) {
        this.businessJpaRepository = businessJpaRepository;
        this.evolutionApi = evolutionApi;
    }

    @EventListener
    @Async("asyncExecutor")
    public void onWhatsAppConnected(BusinessWhatsAppConnectedEvent event) {
        Optional<BusinessJpaEntity> opt = businessJpaRepository.findById(event.businessId());
        if (opt.isEmpty())
            return;

        BusinessJpaEntity business = opt.get();

        // Only send demo once
        if (Boolean.TRUE.equals(business.getDemoOrderSent())) {
            log.debug("Demo order already sent for business={}", event.businessId());
            return;
        }

        String whatsappNumber = business.getWhatsappNumber();
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            log.warn("No whatsappNumber configured for business={}", event.businessId());
            return;
        }

        // 30-second delay so WhatsApp connection stabilizes
        try {
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        String recipientJid = whatsappNumber.replaceAll("[^0-9]", "") + "@s.whatsapp.net";
        String demoMessage = """
                🎉 ¡Hola! Soy el bot de *Orderly*.

                Acabo de conectarme a tu número de WhatsApp. ¡Todo está funcionando!

                Aquí va un pedido de prueba:

                🛒 *Pedido Demo #001*
                • 2x Producto A — $20.000
                • 1x Producto B — $15.000

                💰 *Total:* $55.000
                📍 *Dirección:* Calle Ejemplo 123

                Para confirmar escribe: *confirmar*
                Para cancelar escribe: *cancelar*

                ¡Tu negocio está listo para recibir pedidos reales! 🚀
                """;

        try {
            evolutionApi.sendTextMessage(event.instanceName(), recipientJid, demoMessage);
            business.setDemoOrderSent(true);
            businessJpaRepository.save(business);
            log.info("Demo order sent to business={} number={}", event.businessId(), whatsappNumber);
        } catch (Exception e) {
            log.error("Failed to send demo order for business={}: {}", event.businessId(), e.getMessage());
        }
    }
}
