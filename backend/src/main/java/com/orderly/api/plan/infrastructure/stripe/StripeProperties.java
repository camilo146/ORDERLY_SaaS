package com.orderly.api.plan.infrastructure.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "orderly.stripe")
public record StripeProperties(
        String secretKey,
        String webhookSecret,
        String successUrl,
        String cancelUrl,
        String portalReturnUrl,
        PriceIds prices
) {
    public record PriceIds(
            @DefaultValue("price_placeholder") String starterMonthly,
            @DefaultValue("price_placeholder") String starterAnnual,
            @DefaultValue("price_placeholder") String growthMonthly,
            @DefaultValue("price_placeholder") String growthAnnual,
            @DefaultValue("price_placeholder") String businessMonthly,
            @DefaultValue("price_placeholder") String businessAnnual
    ) {}

    /** Resolves the Stripe price ID for the given plan code and billing cycle. */
    public String resolvePriceId(String planCode, String cycle) {
        boolean annual = "annual".equalsIgnoreCase(cycle);
        return switch (planCode.toLowerCase()) {
            case "starter"  -> annual ? prices.starterAnnual()  : prices.starterMonthly();
            case "growth"   -> annual ? prices.growthAnnual()   : prices.growthMonthly();
            case "business" -> annual ? prices.businessAnnual() : prices.businessMonthly();
            default -> throw new IllegalArgumentException("Unknown plan code: " + planCode);
        };
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.startsWith("sk_test_placeholder");
    }
}
