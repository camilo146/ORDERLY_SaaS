package com.orderly.api.plan.interfaces.rest;

import com.orderly.api.plan.infrastructure.stripe.StripeWebhookHandler;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Receives and verifies Stripe webhook events.
 * The endpoint is under /webhook/** which is already permitted in SecurityConfig (no JWT required).
 *
 * IMPORTANT: Spring must receive the raw payload bytes for signature verification.
 * Using @RequestBody String preserves the raw body without JSON parsing.
 */
@RestController
@RequestMapping("/webhook/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final StripeWebhookHandler webhookHandler;

    public StripeWebhookController(
            @Value("${orderly.stripe.webhook-secret:whsec_placeholder}") String webhookSecret,
            StripeWebhookHandler webhookHandler) {
        this.webhookSecret = webhookSecret;
        this.webhookHandler = webhookHandler;
    }

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payload error");
        }

        // Idempotency: skip already-processed events
        if (webhookHandler.isAlreadyProcessed(event.getId())) {
            log.debug("Skipping already-processed Stripe event: {}", event.getId());
            return ResponseEntity.ok("Already processed");
        }

        try {
            dispatchEvent(event);
            webhookHandler.markProcessed(event.getId(), event.getType());
        } catch (Exception e) {
            log.error("Error processing Stripe event {}: {}", event.getId(), e.getMessage(), e);
            // Return 500 so Stripe retries the event
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Processing error");
        }

        return ResponseEntity.ok("OK");
    }

    private void dispatchEvent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Optional<StripeObject> obj = deserializer.getObject();
                if (obj.isPresent() && obj.get() instanceof Session session) {
                    webhookHandler.handleCheckoutSessionCompleted(session);
                }
            }
            case "invoice.paid" -> {
                Optional<StripeObject> obj = deserializer.getObject();
                if (obj.isPresent() && obj.get() instanceof Invoice invoice) {
                    webhookHandler.handleInvoicePaid(invoice);
                }
            }
            case "invoice.payment_failed" -> {
                Optional<StripeObject> obj = deserializer.getObject();
                if (obj.isPresent() && obj.get() instanceof Invoice invoice) {
                    webhookHandler.handleInvoicePaymentFailed(invoice);
                }
            }
            case "customer.subscription.updated" -> {
                Optional<StripeObject> obj = deserializer.getObject();
                if (obj.isPresent() && obj.get() instanceof com.stripe.model.Subscription sub) {
                    webhookHandler.handleSubscriptionUpdated(sub);
                }
            }
            case "customer.subscription.deleted" -> {
                Optional<StripeObject> obj = deserializer.getObject();
                if (obj.isPresent() && obj.get() instanceof com.stripe.model.Subscription sub) {
                    webhookHandler.handleSubscriptionDeleted(sub);
                }
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }
}
