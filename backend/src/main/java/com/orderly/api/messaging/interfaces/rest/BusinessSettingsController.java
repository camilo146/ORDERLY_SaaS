package com.orderly.api.messaging.interfaces.rest;

import com.orderly.api.messaging.application.EvolutionApiService;
import com.orderly.api.business.application.FindBusinessUseCase;
import com.orderly.api.business.domain.model.Business;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.messaging.application.BusinessTemplateService;
import com.orderly.api.messaging.application.WhatsAppChannelService;
import com.orderly.api.shared.tenant.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Settings endpoints: WhatsApp channel config + message templates per business.
 */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/settings")
public class BusinessSettingsController {

    private final WhatsAppChannelService channelService;
    private final BusinessTemplateService templateService;
    private final FindBusinessUseCase findBusinessUseCase;
    private final EvolutionApiService evolutionApiService;
    private final BusinessRepository businessRepository;
    private final TenantAccessService tenantAccessService;

    public BusinessSettingsController(
            WhatsAppChannelService channelService,
            BusinessTemplateService templateService,
            FindBusinessUseCase findBusinessUseCase,
            EvolutionApiService evolutionApiService,
            BusinessRepository businessRepository,
            TenantAccessService tenantAccessService) {
        this.channelService = channelService;
        this.templateService = templateService;
        this.findBusinessUseCase = findBusinessUseCase;
        this.evolutionApiService = evolutionApiService;
        this.businessRepository = businessRepository;
        this.tenantAccessService = tenantAccessService;
    }

    // ── WhatsApp QR (Evolution API) ─────────────────────────────────────────────

    @GetMapping("/whatsapp-qr")
    public EvolutionApiService.QrResponse getWhatsAppQr(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        try {
            Business business = findBusinessUseCase.findById(businessId);
            return evolutionApiService.getOrCreateQr(businessId, business.slug());
        } catch (Exception e) {
            return new EvolutionApiService.QrResponse(null, "UNAVAILABLE");
        }
    }

    @GetMapping("/whatsapp-status")
    public java.util.Map<String, String> getWhatsAppStatus(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        String state = evolutionApiService.getConnectionState(businessId);
        return java.util.Map.of("state", state);
    }

    @DeleteMapping("/whatsapp-instance")
    public void deleteWhatsAppInstance(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        evolutionApiService.deleteInstance(businessId);
    }

    // ── WhatsApp Channel ────────────────────────────────────────────────────────

    @GetMapping("/whatsapp-channel")
    public WhatsAppChannelService.ChannelConfig getChannel(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return channelService.getOrEmpty(businessId);
    }

    @PostMapping("/whatsapp-channel/confirm")
    public WhatsAppChannelService.ChannelConfig confirmChannel(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        return channelService.confirmConnection(businessId);
    }

    @PutMapping("/whatsapp-channel")
    public WhatsAppChannelService.ChannelConfig saveChannel(
            @PathVariable UUID businessId,
            @Valid @RequestBody ChannelRequest request) {
        tenantAccessService.validateTenantAccess(businessId);
        return channelService.save(businessId, new WhatsAppChannelService.ChannelConfig(
                businessId.toString(),
                request.displayName() != null ? request.displayName() : "",
                request.phoneNumber() != null ? request.phoneNumber() : "",
                request.phoneNumberId() != null ? request.phoneNumberId() : "",
                request.wabaId() != null ? request.wabaId() : "",
                "PENDING_VERIFICATION",
                request.paymentMethods() != null ? request.paymentMethods() : List.of(),
                request.deliveryZones() != null ? request.deliveryZones() : List.of()));
    }

    // ── Message Templates ───────────────────────────────────────────────────────

    @GetMapping("/message-templates")
    public List<BusinessTemplateService.TemplateDto> listTemplates(
            @PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        String businessType;
        try {
            Business biz = findBusinessUseCase.findById(businessId);
            businessType = biz.businessType().name();
        } catch (Exception e) {
            businessType = "GENERIC_RETAIL";
        }
        return templateService.listForBusiness(businessId, businessType);
    }

    @PutMapping("/message-templates/{eventType}")
    public BusinessTemplateService.TemplateDto updateTemplate(
            @PathVariable UUID businessId,
            @PathVariable String eventType,
            @Valid @RequestBody TemplateBodyRequest request) {
        tenantAccessService.validateTenantAccess(businessId);
        String businessType;
        try {
            Business biz = findBusinessUseCase.findById(businessId);
            businessType = biz.businessType().name();
        } catch (Exception e) {
            businessType = "GENERIC_RETAIL";
        }
        return templateService.updateTemplate(businessId, businessType,
                eventType, request.body());
    }

    // ── Request records ─────────────────────────────────────────────────────────

    // ── Bot Identity ────────────────────────────────────────────────────────────

    @GetMapping("/bot-identity")
    public BotIdentityResponse getBotIdentity(@PathVariable UUID businessId) {
        tenantAccessService.validateTenantAccess(businessId);
        Business business = findBusinessUseCase.findById(businessId);
        return new BotIdentityResponse(business.botName(), business.botEmoji());
    }

    @PutMapping("/bot-identity")
    public BotIdentityResponse saveBotIdentity(
            @PathVariable UUID businessId,
            @Valid @RequestBody BotIdentityRequest request) {
        tenantAccessService.validateTenantAccess(businessId);
        String name = (request.botName() != null && !request.botName().isBlank()) ? request.botName().trim()
                : "Orderly";
        String emoji = (request.botEmoji() != null && !request.botEmoji().isBlank()) ? request.botEmoji().trim() : "🤖";
        businessRepository.updateBotIdentity(businessId, name, emoji);
        return new BotIdentityResponse(name, emoji);
    }

    public record BotIdentityRequest(String botName, String botEmoji) {
    }

    public record BotIdentityResponse(String botName, String botEmoji) {
    }

    // ── Request records ─────────────────────────────────────────────────────────

    public record ChannelRequest(
            String displayName,
            String phoneNumber,
            String phoneNumberId,
            String wabaId,
            List<String> paymentMethods,
            List<String> deliveryZones) {
    }

    public record TemplateBodyRequest(@NotBlank String body) {
    }
}
