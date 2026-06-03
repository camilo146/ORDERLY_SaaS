package com.orderly.api.messaging.interfaces.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.orderly.api.messaging.application.WhatsAppMessageDispatcher;
import com.orderly.api.shared.config.EvolutionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Receives inbound webhooks from Evolution API for each business instance.
 * URL pattern: POST /webhook/whatsapp/{slug}
 *
 * Evolution API sends events: MESSAGES_UPSERT, CONNECTION_UPDATE,
 * QRCODE_UPDATED
 */
@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    // [SECURITY FIX VUL-01] Header enviado por Evolution API con la firma
    // HMAC-SHA256 del body
    private static final String SIGNATURE_HEADER = "x-hub-signature-256";

    private final WhatsAppMessageDispatcher dispatcher;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(WhatsAppMessageDispatcher dispatcher,
            EvolutionProperties evolutionProperties,
            ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        // [SECURITY FIX VUL-01] Cargar el secret desde config, nunca desde código
        // fuente
        this.webhookSecret = evolutionProperties.webhookSecret();
        this.objectMapper = objectMapper;
    }

    /**
     * Main webhook receiver.
     * Evolution API POSTs here for every event on the instance.
     */
    @PostMapping("/{slug}")
    public ResponseEntity<Void> receive(
            @PathVariable String slug,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestBody String rawBody) {

        // [SECURITY FIX VUL-01] Verificar firma HMAC-SHA256 antes de procesar.
        // Evita que actores externos inyecten eventos falsos al chatbot para crear
        // órdenes fraudulentas o ejecutar lógica de negocio no autorizada.
        if (!isSignatureValid(rawBody, signature)) {
            log.warn("Webhook signature verification FAILED for slug={} — request rejected", slug);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Deserializar el body ya validado
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        } catch (Exception e) {
            log.warn("Webhook body parse error for slug={}: {}", slug, e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        if (payload.event() == null) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Webhook received slug={} event={} instance={}", slug, payload.event(), payload.instance());

        try {
            String event = normalizeEvent(payload.event());
            switch (event) {
                case "messages_upsert" -> handleMessagesUpsert(slug, payload);
                case "connection_update" -> handleConnectionUpdate(slug, payload);
                case "qrcode_updated" -> log.debug("QR updated for slug={}", slug);
                default ->
                    log.debug("Unhandled Evolution event: {} (normalized={}) for slug={}", payload.event(), event,
                            slug);
            }
        } catch (Exception e) {
            // Never return 5xx — Evolution API would retry indefinitely
            log.error("Error processing webhook for slug={}: {}", slug, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void handleMessagesUpsert(String slug, WebhookPayload payload) {
        if (payload.data() == null)
            return;

        // Evolution API v1.8.x sends either:
        // a) data.messages[] array (webhook_by_events=true or batch)
        // b) flat object in data: {key, message, pushName, ...} (single-message event)
        List<?> messages;
        Object messagesRaw = payload.data().get("messages");
        if (messagesRaw instanceof List<?> msgList) {
            messages = msgList;
        } else if (payload.data().containsKey("key") && payload.data().containsKey("message")) {
            // Flat structure: treat data itself as the single message
            log.debug("messages_upsert flat payload for slug={}, treating data as single message", slug);
            messages = List.of(payload.data());
        } else {
            log.warn("messages_upsert for slug={} but no messages array in payload data keys={}", slug,
                    payload.data().keySet());
            return;
        }

        for (Object msgObj : messages) {
            if (!(msgObj instanceof Map<?, ?> msg))
                continue;

            String remoteJid = extractString(msg, "key", "remoteJid");
            boolean fromMe = Boolean.TRUE.equals(extractValue(msg, "key", "fromMe"));
            log.debug("Processing message slug={} remoteJid={} fromMe={}", slug, remoteJid, fromMe);
            if (fromMe)
                continue; // Ignore outbound messages

            if (remoteJid != null && remoteJid.endsWith("@lid")) {
                log.debug("LID inbound diagnostic slug={} source={} owner={}",
                        slug, msg.get("source"), msg.get("owner"));
            }

            String replyJid = resolveReplyJid(msg, remoteJid);
            if (replyJid == null || replyJid.isBlank()) {
                log.warn("Could not resolve replyJid for slug={} remoteJid={}", slug, remoteJid);
                continue;
            }

            String text = extractIncomingText(msg);
            String mediaUrl = extractIncomingMedia(msg);
            if ((text == null || text.isBlank()) && mediaUrl == null) {
                log.debug("No text/media in message from slug={} remoteJid={} — ignoring", slug, remoteJid);
                continue;
            }

            String dispatchText = (text != null && !text.isBlank()) ? text : "[MEDIA:" + mediaUrl + "]";

            // [SECURITY FIX VUL-10] No logear el contenido completo del mensaje (PII/GDPR).
            // El número se anonimiza para auditoría sin exponer datos personales.
            String maskedFrom = maskJid(remoteJid);
            String maskedReply = maskJid(replyJid);
            log.info("Inbound WA message from={} replyTo={} slug={} text_len={} hasMedia={}", maskedFrom, maskedReply,
                    slug,
                    dispatchText.length(), mediaUrl != null);
            String evolutionInstance = (payload.instance() != null && !payload.instance().isBlank())
                    ? payload.instance()
                    : "orderly-" + slug;
            dispatcher.dispatch(slug, evolutionInstance, replyJid, dispatchText);
        }
    }

    private void handleConnectionUpdate(String slug, WebhookPayload payload) {
        if (payload.data() == null)
            return;
        Object stateRaw = payload.data().get("state");
        if (stateRaw instanceof String state) {
            log.info("WhatsApp connection state change: slug={} state={}", slug, state);
            dispatcher.onConnectionUpdate(slug, state);
        }
    }

    // -------------------------------------------------------------------------
    // Extraction helpers — resilient to Evolution API payload variations
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String extractString(Map<?, ?> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> m))
                return null;
            current = m.get(key);
        }
        return current instanceof String s ? s : null;
    }

    @SuppressWarnings("unchecked")
    private Object extractValue(Map<?, ?> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> m))
                return null;
            current = m.get(key);
        }
        return current;
    }

    private String extractIncomingText(Map<?, ?> msg) {
        // Evolution API nests text in message.conversation or
        // message.extendedTextMessage.text
        Object messageSection = msg.get("message");
        if (!(messageSection instanceof Map<?, ?> message))
            return null;

        Object conversation = message.get("conversation");
        if (conversation instanceof String s && !s.isBlank())
            return s;

        if (message.get("extendedTextMessage") instanceof Map<?, ?> ext) {
            Object text = ext.get("text");
            if (text instanceof String s)
                return s;
        }
        return null;
    }

    private String extractIncomingMedia(Map<?, ?> msg) {
        Object messageSection = msg.get("message");
        if (!(messageSection instanceof Map<?, ?> message))
            return null;
        if (message.get("imageMessage") instanceof Map<?, ?> img) {
            Object url = img.get("url");
            if (url instanceof String s && !s.isBlank())
                return s;
            Object directPath = img.get("directPath");
            if (directPath instanceof String dp && !dp.isBlank())
                return dp;
        }
        return null;
    }

    private String normalizeEvent(String rawEvent) {
        if (rawEvent == null) {
            return "";
        }
        return rawEvent.trim().toLowerCase().replace('.', '_').replace('-', '_');
    }

    private String resolveReplyJid(Map<?, ?> msg, String remoteJid) {
        if (remoteJid == null || remoteJid.isBlank()) {
            return null;
        }
        if (!remoteJid.endsWith("@lid")) {
            return remoteJid;
        }

        // Evolution payloads vary by version/config. Try many common fields where
        // the real phone JID can appear when remoteJid uses @lid.
        String[] candidates = new String[] {
                extractString(msg, "sender_pn"),
                extractString(msg, "senderPn"),
                extractString(msg, "sender"),
                extractString(msg, "participant"),
                extractString(msg, "participantPn"),
                extractString(msg, "participant_pn"),
                extractString(msg, "key", "participant"),
                extractString(msg, "key", "participantPn"),
                extractString(msg, "key", "participant_pn"),
                extractString(msg, "key", "remoteJidAlt"),
                extractString(msg, "key", "remoteJidAltPn"),
                extractString(msg, "source"),
                extractString(msg, "owner"),
                extractString(msg, "source", "chatId"),
                extractString(msg, "source", "sender"),
                extractString(msg, "source", "senderPn"),
                extractString(msg, "source", "sender_pn"),
                extractString(msg, "source", "remoteJid")
        };

        for (String candidate : candidates) {
            String normalized = normalizeToWhatsAppJid(candidate);
            if (normalized != null && !normalized.endsWith("@lid")) {
                return normalized;
            }
        }

        Object keyObj = msg.get("key");
        Object keySet = keyObj instanceof Map<?, ?> m ? m.keySet() : keyObj;
        log.warn("Could not resolve @lid reply jid remoteJid={} candidates={} msgKeys={} keyKeys={}",
                remoteJid, java.util.Arrays.toString(candidates), msg.keySet(), keySet);

        // Fallback to original value if no conversion data is available.
        return remoteJid;
    }

    private String normalizeToWhatsAppJid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains("@")) {
            return trimmed;
        }
        if (trimmed.matches("\\d{8,15}")) {
            return trimmed + "@s.whatsapp.net";
        }
        return null;
    }

    // ── Webhook HMAC-SHA256 signature verification ────────────────────────────

    /**
     * [SECURITY FIX VUL-10] Enmascara JIDs para logs cumpliendo GDPR/LGPD.
     * Muestra sólo los últimos 4 dígitos del número: +57****7890@s.whatsapp.net
     */
    private String maskJid(String jid) {
        if (jid == null)
            return "null";
        int atPos = jid.indexOf('@');
        String number = atPos > 0 ? jid.substring(0, atPos) : jid;
        String suffix = atPos > 0 ? jid.substring(atPos) : "";
        if (number.length() <= 4)
            return "****" + suffix;
        return number.substring(0, 2) + "****" + number.substring(number.length() - 4) + suffix;
    }

    /**
     * Verifies that the webhook originates from Evolution API using HMAC-SHA256
     * with constant-time comparison (MessageDigest.isEqual) to prevent timing attacks.
     *
     * If EVOLUTION_API_WEBHOOK_SECRET is not set (empty), ALL requests are rejected.
     * There is no "degraded mode" bypass — an unconfigured secret is a misconfiguration,
     * not a valid operational state.
     */
    private boolean isSignatureValid(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("[SECURITY] EVOLUTION_API_WEBHOOK_SECRET is not configured. " +
                    "All webhook requests are REJECTED until the secret is set. " +
                    "Set the EVOLUTION_API_WEBHOOK_SECRET environment variable.");
            return false;
        }
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("[SECURITY] Webhook request missing or malformed x-hub-signature-256 header.");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] received = HexFormat.of().parseHex(signature.substring(7));
            return MessageDigest.isEqual(expected, received);
        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            log.error("[SECURITY] HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    // ── Payload record ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WebhookPayload(
            @JsonProperty("event") String event,
            @JsonProperty("data") Map<String, Object> data,
            @JsonProperty("instance") String instance) {
    }
}
