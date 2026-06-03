package com.orderly.api.messaging.application;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.orderly.api.shared.config.EvolutionProperties;

/**
 * Client for the Evolution API — free open-source WhatsApp gateway.
 * Generates real WhatsApp QR codes for scanning.
 *
 * Docs: https://doc.evolution-api.com
 */
@Service
public class EvolutionApiService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiService.class);

    private final String baseUrl;
    private final String apiKey;
    private final String appBaseUrl;
    private final RestTemplate restTemplate;

    public EvolutionApiService(EvolutionProperties props) {
        this.baseUrl = props.baseUrl();
        this.apiKey = props.apiKey();
        this.appBaseUrl = props.appBaseUrl();
        this.restTemplate = new RestTemplate();
    }

    /** True if the Evolution API server is reachable. */
    public boolean isAvailable() {
        try {
            restTemplate.exchange(baseUrl + "/", HttpMethod.GET, withHeaders(null), String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates (or re-uses) a WhatsApp instance for this business and returns
     * the QR code as a base64 data URL (data:image/png;base64,...).
     * Returns null if the instance is already connected.
     *
     * @param businessId business UUID
     * @param slug       business slug used as human-readable instance name
     */
    public QrResponse getOrCreateQr(UUID businessId, String slug) {
        String instanceToken = resolveInstanceToken(businessId);
        String instanceName = resolveInstanceNameByTokenOrDefault(instanceToken, resolveInstanceName(slug, businessId));

        // If already connected, no QR is needed.
        String currentState = getConnectionStateByInstance(instanceName);
        if ("open".equalsIgnoreCase(currentState)) {
            try {
                if (slug != null && !slug.isBlank()) {
                    configureWebhook(instanceName, slug);
                }
            } catch (Exception ignored) {
                // Non-fatal: connection is already open, but we still try to self-heal the
                // webhook.
            }
            return new QrResponse(null, "CONNECTED");
        }

        // Try to connect (get existing QR)
        try {
            ResponseEntity<ConnectResponse> response = restTemplate.exchange(
                    baseUrl + "/instance/connect/" + instanceName,
                    HttpMethod.GET, withHeaders(null), ConnectResponse.class);
            ConnectResponse body = response.getBody();
            if (body != null && body.base64() != null) {
                return new QrResponse(body.base64(), "CONNECTING");
            }
            String stateAfterConnect = getConnectionStateByInstance(instanceName);
            if ("open".equalsIgnoreCase(stateAfterConnect)) {
                return new QrResponse(null, "CONNECTED");
            }
        } catch (Exception ignored) {
            // Instance doesn't exist yet, create it
        }

        // Create new instance
        try {
            Map<String, Object> createBody = Map.of(
                    "instanceName", instanceName,
                    "token", instanceToken,
                    "integration", "WHATSAPP-BAILEYS",
                    "qrcode", true);
            ResponseEntity<CreateInstanceResponse> response = restTemplate.exchange(
                    baseUrl + "/instance/create",
                    HttpMethod.POST, withHeaders(createBody), CreateInstanceResponse.class);
            CreateInstanceResponse body = response.getBody();
            if (body != null) {
                // Attempt to auto-configure webhook after instance creation
                try {
                    configureWebhook(instanceName, slug);
                } catch (Exception ignored) {
                }
                if (body.qrcode() != null && body.qrcode().base64() != null) {
                    return new QrResponse(body.qrcode().base64(), "CONNECTING");
                }
            }
        } catch (Exception e) {
            // Legacy recovery: if token already exists, find instance by token and request
            // QR.
            if (e.getMessage() != null && e.getMessage().contains("Token already exists")) {
                String existingInstanceName = resolveInstanceNameByTokenOrDefault(instanceToken, instanceName);
                try {
                    ResponseEntity<ConnectResponse> response = restTemplate.exchange(
                            baseUrl + "/instance/connect/" + existingInstanceName,
                            HttpMethod.GET, withHeaders(null), ConnectResponse.class);
                    ConnectResponse body = response.getBody();
                    if (body != null && body.base64() != null) {
                        return new QrResponse(body.base64(), "CONNECTING");
                    }
                    String stateAfterRecovery = getConnectionStateByInstance(existingInstanceName);
                    if ("open".equalsIgnoreCase(stateAfterRecovery)) {
                        return new QrResponse(null, "CONNECTED");
                    }
                } catch (Exception ignored) {
                }
                return new QrResponse(null, "UNAVAILABLE");
            }
            throw new RuntimeException("Evolution API error: " + e.getMessage());
        }

        return new QrResponse(null, "UNAVAILABLE");
    }

    /** Backward-compat overload when slug is not available. */
    public QrResponse getOrCreateQr(UUID businessId) {
        return getOrCreateQr(businessId, null);
    }

    /**
     * Returns the current connection state of the WhatsApp instance.
     * Possible values: "open" (connected), "connecting", "close".
     */
    public String getConnectionState(UUID businessId) {
        String instanceToken = resolveInstanceToken(businessId);
        String instanceName = resolveInstanceNameByTokenOrDefault(
                instanceToken,
                resolveInstanceName(null, businessId));
        try {
            ResponseEntity<StateResponse> response = restTemplate.exchange(
                    baseUrl + "/instance/connectionState/" + instanceName,
                    HttpMethod.GET, withHeaders(null), StateResponse.class);
            StateResponse body = response.getBody();
            if (body != null && body.instance() != null) {
                return body.instance().state();
            }
        } catch (Exception e) {
            return "close";
        }
        return "close";
    }

    /** Disconnects and deletes the WhatsApp instance for this business. */
    public void deleteInstance(UUID businessId) {
        String instanceToken = resolveInstanceToken(businessId);
        String instanceName = resolveInstanceNameByTokenOrDefault(
                instanceToken,
                resolveInstanceName(null, businessId));
        try {
            restTemplate.exchange(
                    baseUrl + "/instance/delete/" + instanceName,
                    HttpMethod.DELETE, withHeaders(null), String.class);
        } catch (Exception ignored) {
        }
    }

    /**
     * Sends a plain text message to a recipient WhatsApp number.
     *
     * @param instanceName Evolution API instance name
     * @param recipientJid phone number + @s.whatsapp.net (e.g.
     *                     "573001234567@s.whatsapp.net") or just the number
     * @param text         message body
     */
    public void sendTextMessage(String instanceName, String recipientJid, String text) {
        // Ensure full JID (with @suffix) so Evolution skips its checkIsWhatsApp
        // validation
        // Evolution v2.x handles @lid JIDs natively — no resolution needed
        String jid = (recipientJid != null && recipientJid.contains("@"))
                ? recipientJid
                : recipientJid + "@s.whatsapp.net";

        // Evolution v2 expects plain number for normal WhatsApp JIDs, but @lid
        // identifiers must be preserved as-is.
        String destination = jid;
        if (jid.endsWith("@s.whatsapp.net")) {
            destination = jid.substring(0, jid.indexOf('@'));
        }

        Map<String, Object> body = Map.of(
                "number", destination,
                "text", text,
                "options", Map.of("delay", 1200));

        log.info("sendTextMessage instance={} to={} text_len={}", instanceName, jid, text.length());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/message/sendText/" + instanceName,
                    HttpMethod.POST, withHeaders(body), String.class);
            log.info("sendTextMessage OK status={} instance={} to={}", response.getStatusCode(), instanceName, jid);
        } catch (HttpClientErrorException e) {
            log.error("sendTextMessage FAILED instance={} to={} httpStatus={} body={}",
                    instanceName, jid, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("sendTextMessage FAILED instance={} to={} error={}", instanceName, jid, e.getMessage());
            throw e;
        }
    }

    /**
     * Sends an image message using a publicly reachable image URL.
     */
    public void sendImageMessage(String instanceName, String recipientJid, String imageUrl, String caption) {
        String publicUrl = toPublicUrl(imageUrl);
        if (publicUrl == null) {
            return;
        }

        Map<String, Object> body = Map.of(
                "number", recipientJid,
                "mediatype", "image",
                "media", publicUrl,
                "caption", caption == null ? "" : caption);

        restTemplate.exchange(
                baseUrl + "/message/sendMedia/" + instanceName,
                HttpMethod.POST, withHeaders(body), String.class);
    }

    /**
     * Configures the Evolution API webhook for a specific instance to point back
     * to this application's webhook endpoint.
     */
    public void configureWebhook(String instanceName, String businessSlug) {
        String webhookUrl = appBaseUrl + "/webhook/whatsapp/" + businessSlug;
        // Evolution v2 requires the config nested under "webhook" key (camelCase
        // fields)
        Map<String, Object> webhookConfig = Map.of(
                "url", webhookUrl,
                "enabled", true,
                "webhookByEvents", false,
                "webhookBase64", false,
                "events", List.of(
                        "MESSAGES_UPSERT",
                        "CONNECTION_UPDATE",
                        "QRCODE_UPDATED"));
        Map<String, Object> body = Map.of("webhook", webhookConfig);
        try {
            restTemplate.exchange(
                    baseUrl + "/webhook/set/" + instanceName,
                    HttpMethod.POST, withHeaders(body), String.class);
        } catch (Exception e) {
            // Non-fatal: webhook config can be retried
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Build instance name: prefer slug, fall back to UUID. */
    private String resolveInstanceName(String slug, UUID businessId) {
        if (slug != null && !slug.isBlank()) {
            return "orderly-" + slug;
        }
        return "orderly-" + businessId;
    }

    /**
     * Finds an existing instance name by business token (supports v2 flat response
     * and v1 nested response).
     */
    private String resolveInstanceNameByTokenOrDefault(String token, String defaultName) {
        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    baseUrl + "/instance/fetchInstances",
                    HttpMethod.GET,
                    withHeaders(null),
                    List.class);
            List<?> rows = response.getBody();
            if (rows == null) {
                return defaultName;
            }

            for (Object row : rows) {
                if (!(row instanceof Map<?, ?> rowMap)) {
                    continue;
                }

                // Evolution v2: flat structure { "name": "...", "token": "..." }
                String instanceName = asString(rowMap.get("name"));
                String instanceToken = asString(rowMap.get("token"));
                if (instanceName != null && token.equals(instanceToken)) {
                    return instanceName;
                }

                // Evolution v1 fallback: nested { "instance": { "instanceName": "...", ... } }
                if (rowMap.get("instance") instanceof Map<?, ?> instanceMap) {
                    instanceName = asString(instanceMap.get("instanceName"));
                    String instanceApiKey = asString(instanceMap.get("apikey"));
                    String integrationToken = null;
                    Object integrationObj = instanceMap.get("integration");
                    if (integrationObj instanceof Map<?, ?> integrationMap) {
                        integrationToken = asString(integrationMap.get("token"));
                    }
                    boolean tokenMatch = token.equals(instanceApiKey)
                            || (integrationToken != null && token.equals(integrationToken));
                    if (tokenMatch && instanceName != null && !instanceName.isBlank()) {
                        return instanceName;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return defaultName;
    }

    private String asString(Object value) {
        if (value instanceof String s) {
            return s;
        }
        return null;
    }

    private String getConnectionStateByInstance(String instanceName) {
        try {
            ResponseEntity<StateResponse> response = restTemplate.exchange(
                    baseUrl + "/instance/connectionState/" + instanceName,
                    HttpMethod.GET, withHeaders(null), StateResponse.class);
            StateResponse body = response.getBody();
            if (body != null && body.instance() != null && body.instance().state() != null) {
                return body.instance().state();
            }
        } catch (Exception ignored) {
        }
        return "close";
    }

    /** Stable token expected by Evolution create API in v1.x line. */
    private String resolveInstanceToken(UUID businessId) {
        return businessId.toString().replace("-", "");
    }

    private String toPublicUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return appBaseUrl + trimmed;
        }
        return appBaseUrl + "/" + trimmed;
    }

    private <T> HttpEntity<T> withHeaders(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        headers.set("Content-Type", "application/json");
        return new HttpEntity<>(body, headers);
    }

    // ── Response records ──────────────────────────────────────────────────────

    public record QrResponse(String base64, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConnectResponse(String code, String base64) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateInstanceResponse(
            @JsonProperty("qrcode") QrCodeData qrcode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QrCodeData(String code, String base64) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StateResponse(@JsonProperty("instance") InstanceState instance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstanceState(String state) {
    }

    // ── LID resolution ────────────────────────────────────────────────────────

    /**
     * Tries to resolve a WhatsApp @lid JID to a real @s.whatsapp.net JID.
     *
     * Evolution 1.8.x exposes contact/profile lookups in /chat/* endpoints, not
     * /contact/*. We first try /chat/fetchProfile (POST), then /chat/findChats
     * (GET) as fallback.
     *
     * @return full JID like "573001112233@s.whatsapp.net", or null if not
     *         resolvable
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String resolveLidJid(String instanceName, String lidJid) {
        String numericId = lidJid.contains("@") ? lidJid.substring(0, lidJid.indexOf('@')) : lidJid;
        log.info("resolveLidJid: attempting to resolve {} for instance {}", lidJid, instanceName);

        // Attempt 1: fetchProfile (POST /chat/fetchProfile/{instance})
        try {
            Map<String, Object> requestBody = Map.of("number", numericId);
            ResponseEntity<Map> resp = restTemplate.exchange(
                    baseUrl + "/chat/fetchProfile/" + instanceName,
                    HttpMethod.POST, withHeaders(requestBody), Map.class);
            Map<?, ?> body = resp.getBody();
            if (body != null) {
                log.info("resolveLidJid fetchProfile for {} returned keys={} body={}", lidJid, body.keySet(), body);
                String resolved = extractRealJidFromContactMap(body, lidJid);
                if (resolved != null)
                    return resolved;
            }
        } catch (Exception e) {
            log.debug("resolveLidJid fetchProfile failed for {}: {}", lidJid, e.getMessage());
        }

        // Attempt 2: scan chats looking for matching id/lid
        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                    baseUrl + "/chat/findChats/" + instanceName,
                    HttpMethod.GET, withHeaders(null), List.class);
            List<?> chats = resp.getBody();
            if (chats != null) {
                log.info("resolveLidJid findChats for instance {} returned {} chats", instanceName,
                        chats.size());
                for (Object c : chats) {
                    if (!(c instanceof Map<?, ?> chat))
                        continue;
                    Object id = chat.get("id");
                    Object lid = chat.get("lid");
                    if (lidJid.equals(id) || lidJid.equals(lid) || numericId.equals(id)) {
                        log.info("resolveLidJid found matching chat: {}", chat);
                        String resolved = extractRealJidFromContactMap(chat, lidJid);
                        if (resolved != null)
                            return resolved;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("resolveLidJid findChats failed for {}: {}", lidJid, e.getMessage());
        }

        log.warn("resolveLidJid: could not resolve @lid JID {} - upgrade Evolution to v2.x for full LID support",
                lidJid);
        return null;
    }

    private String extractRealJidFromContactMap(Map<?, ?> contact, String lidJid) {
        // Check well-known field names that Evolution uses for the real phone JID
        for (String field : new String[] { "wuid", "jid", "phone", "number", "remoteJid" }) {
            Object val = contact.get(field);
            if (val instanceof String s && !s.isBlank() && !s.endsWith("@lid")) {
                // Make sure it looks like a real phone JID
                if (s.contains("@s.whatsapp.net") || s.matches("\\d{8,15}")) {
                    String resolved = s.contains("@") ? s : s + "@s.whatsapp.net";
                    log.info("extractRealJidFromContactMap: {} → {} (via field '{}')", lidJid, resolved, field);
                    return resolved;
                }
            }
        }
        return null;
    }
}
