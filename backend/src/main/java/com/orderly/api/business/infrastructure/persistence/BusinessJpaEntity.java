package com.orderly.api.business.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for tenant businesses.
 */
@Entity
@Table(name = "businesses")
public class BusinessJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "business_type", nullable = false)
    private String businessType;

    @Column(nullable = false)
    private String status;

    @Column(name = "country_code", nullable = false)
    private String countryCode;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    // -----------------------------------------------------------------------
    // v2.1 fields (added via V2 Flyway migration)
    // -----------------------------------------------------------------------

    @Column(name = "plan_id")
    private UUID planId;

    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    @Column(name = "whatsapp_instance_id", length = 100)
    private String whatsappInstanceId;

    @Column(name = "whatsapp_token", columnDefinition = "TEXT")
    private String whatsappToken;

    @Column(name = "demo_order_sent")
    private Boolean demoOrderSent = false;

    @Column(name = "onboarding_step")
    private Integer onboardingStep = 0;

    @Column(name = "bot_name", length = 80)
    private String botName = "Orderly";

    @Column(name = "bot_emoji", length = 10)
    private String botEmoji = "🤖";

    @Column(name = "payment_nequi", length = 60)
    private String paymentNequi;

    @Column(name = "payment_bank_name", length = 80)
    private String paymentBankName;

    @Column(name = "payment_bank_account", length = 60)
    private String paymentBankAccount;

    @Column(name = "payment_bank_holder", length = 100)
    private String paymentBankHolder;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    protected BusinessJpaEntity() {
    }

    public BusinessJpaEntity(
            UUID id,
            UUID ownerId,
            String name,
            String slug,
            String businessType,
            String status,
            String countryCode,
            String currencyCode,
            String timezone,
            OffsetDateTime createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.slug = slug;
        this.businessType = businessType;
        this.status = status;
        this.countryCode = countryCode;
        this.currencyCode = currencyCode;
        this.timezone = timezone;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getStatus() {
        return status;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getTimezone() {
        return timezone;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getWhatsappInstanceId() {
        return whatsappInstanceId;
    }

    public void setWhatsappInstanceId(String whatsappInstanceId) {
        this.whatsappInstanceId = whatsappInstanceId;
    }

    public String getWhatsappToken() {
        return whatsappToken;
    }

    public void setWhatsappToken(String whatsappToken) {
        this.whatsappToken = whatsappToken;
    }

    public Boolean getDemoOrderSent() {
        return demoOrderSent;
    }

    public void setDemoOrderSent(Boolean demoOrderSent) {
        this.demoOrderSent = demoOrderSent;
    }

    public Integer getOnboardingStep() {
        return onboardingStep;
    }

    public void setOnboardingStep(Integer onboardingStep) {
        this.onboardingStep = onboardingStep;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotEmoji() {
        return botEmoji;
    }

    public void setBotEmoji(String botEmoji) {
        this.botEmoji = botEmoji;
    }

    public String getPaymentNequi() {
        return paymentNequi;
    }

    public void setPaymentNequi(String paymentNequi) {
        this.paymentNequi = paymentNequi;
    }

    public String getPaymentBankName() {
        return paymentBankName;
    }

    public void setPaymentBankName(String paymentBankName) {
        this.paymentBankName = paymentBankName;
    }

    public String getPaymentBankAccount() {
        return paymentBankAccount;
    }

    public void setPaymentBankAccount(String paymentBankAccount) {
        this.paymentBankAccount = paymentBankAccount;
    }

    public String getPaymentBankHolder() {
        return paymentBankHolder;
    }

    public void setPaymentBankHolder(String paymentBankHolder) {
        this.paymentBankHolder = paymentBankHolder;
    }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
}
