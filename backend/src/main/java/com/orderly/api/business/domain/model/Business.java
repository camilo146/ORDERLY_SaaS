package com.orderly.api.business.domain.model;

import com.orderly.api.shared.domain.DomainException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Business aggregate root for tenant setup.
 */
public final class Business {

    private final UUID id;
    private final UUID ownerId;
    private final String name;
    private final String slug;
    private final BusinessType businessType;
    private final BusinessStatus status;
    private final String countryCode;
    private final String currencyCode;
    private final String timezone;
    private final OffsetDateTime createdAt;
    private final String botName;
    private final String botEmoji;
    private final String paymentNequi;
    private final String paymentBankName;
    private final String paymentBankAccount;
    private final String paymentBankHolder;

    private Business(
            UUID id,
            UUID ownerId,
            String name,
            String slug,
            BusinessType businessType,
            BusinessStatus status,
            String countryCode,
            String currencyCode,
            String timezone,
            OffsetDateTime createdAt,
            String botName,
            String botEmoji,
            String paymentNequi,
            String paymentBankName,
            String paymentBankAccount,
            String paymentBankHolder) {
        this.id = Objects.requireNonNull(id);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.name = requireText(name, "Business name is required.");
        this.slug = requireText(slug, "Business slug is required.");
        this.businessType = Objects.requireNonNull(businessType);
        this.status = Objects.requireNonNull(status);
        this.countryCode = requireText(countryCode, "Country code is required.");
        this.currencyCode = requireText(currencyCode, "Currency code is required.");
        this.timezone = requireText(timezone, "Timezone is required.");
        this.createdAt = Objects.requireNonNull(createdAt);
        this.botName = (botName != null && !botName.isBlank()) ? botName.trim() : "Orderly";
        this.botEmoji = (botEmoji != null && !botEmoji.isBlank()) ? botEmoji.trim() : "🤖";
        this.paymentNequi = paymentNequi;
        this.paymentBankName = paymentBankName;
        this.paymentBankAccount = paymentBankAccount;
        this.paymentBankHolder = paymentBankHolder;
    }

    public static Business create(
            UUID ownerId,
            String name,
            String slug,
            BusinessType businessType,
            String countryCode,
            String currencyCode,
            String timezone) {
        return new Business(
                UUID.randomUUID(),
                ownerId,
                name,
                slug,
                businessType,
                BusinessStatus.SETUP,
                countryCode,
                currencyCode,
                timezone,
                OffsetDateTime.now(),
                "Orderly",
                "🤖",
                null, null, null, null);
    }

    public static Business restore(
            UUID id,
            UUID ownerId,
            String name,
            String slug,
            BusinessType businessType,
            BusinessStatus status,
            String countryCode,
            String currencyCode,
            String timezone,
            OffsetDateTime createdAt) {
        return new Business(id, ownerId, name, slug, businessType, status, countryCode, currencyCode, timezone,
                createdAt, "Orderly", "🤖", null, null, null, null);
    }

    public static Business restore(
            UUID id,
            UUID ownerId,
            String name,
            String slug,
            BusinessType businessType,
            BusinessStatus status,
            String countryCode,
            String currencyCode,
            String timezone,
            OffsetDateTime createdAt,
            String botName,
            String botEmoji) {
        return new Business(id, ownerId, name, slug, businessType, status, countryCode, currencyCode, timezone,
                createdAt, botName, botEmoji, null, null, null, null);
    }

    public static Business restore(
            UUID id,
            UUID ownerId,
            String name,
            String slug,
            BusinessType businessType,
            BusinessStatus status,
            String countryCode,
            String currencyCode,
            String timezone,
            OffsetDateTime createdAt,
            String botName,
            String botEmoji,
            String paymentNequi,
            String paymentBankName,
            String paymentBankAccount,
            String paymentBankHolder) {
        return new Business(id, ownerId, name, slug, businessType, status, countryCode, currencyCode, timezone,
                createdAt, botName, botEmoji, paymentNequi, paymentBankName, paymentBankAccount, paymentBankHolder);
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String name() {
        return name;
    }

    public String slug() {
        return slug;
    }

    public BusinessType businessType() {
        return businessType;
    }

    public BusinessStatus status() {
        return status;
    }

    public String countryCode() {
        return countryCode;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String timezone() {
        return timezone;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public String botName() {
        return botName;
    }

    public String botEmoji() {
        return botEmoji;
    }

    public String paymentNequi() {
        return paymentNequi;
    }

    public String paymentBankName() {
        return paymentBankName;
    }

    public String paymentBankAccount() {
        return paymentBankAccount;
    }

    public String paymentBankHolder() {
        return paymentBankHolder;
    }

    public Business withStatus(BusinessStatus newStatus) {
        return new Business(id, ownerId, name, slug, businessType, newStatus, countryCode, currencyCode, timezone,
                createdAt, botName, botEmoji, paymentNequi, paymentBankName, paymentBankAccount, paymentBankHolder);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainException(message);
        }
        return value.trim();
    }
}
