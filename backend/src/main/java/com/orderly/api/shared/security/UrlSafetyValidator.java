package com.orderly.api.shared.security;

import com.orderly.api.shared.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates user-supplied URLs to prevent Server-Side Request Forgery (SSRF).
 *
 * Blocks requests to private networks, loopback addresses, link-local addresses
 * (including cloud metadata endpoints like 169.254.169.254), and internal
 * Docker service hostnames.
 *
 * Call {@link #validatePublicUrl(String, String)} before passing any user-provided
 * URL to an outbound HTTP client or external service.
 */
public final class UrlSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlSafetyValidator.class);

    // Internal Docker service names that must never be reachable via user-supplied URLs.
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "metadata.google.internal",
            "redis",
            "postgres",
            "mongo",
            "evolution-api",
            "orderly-postgres",
            "orderly-redis",
            "orderly-mongo",
            "orderly-evolution"
    );

    private UrlSafetyValidator() {
    }

    /**
     * Validates that {@code url} is a safe, publicly reachable HTTP/HTTPS URL.
     *
     * @param url       the URL string to validate (may be null/blank — returns null)
     * @param fieldName human-readable field name used in error messages
     * @return the trimmed URL if valid
     * @throws DomainException if the URL fails any safety check
     */
    public static String validatePublicUrl(String url, String fieldName) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String trimmed = url.strip();

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new DomainException(fieldName + ": formato de URL inválido.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new DomainException(fieldName + ": solo se permiten URLs con esquema http o https.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new DomainException(fieldName + ": la URL no contiene un host válido.");
        }

        String hostLower = host.toLowerCase();
        if (BLOCKED_HOSTNAMES.contains(hostLower)) {
            log.warn("[SECURITY] SSRF blocked: host '{}' is in blocklist (field={})", host, fieldName);
            throw new DomainException(fieldName + ": URL no permitida.");
        }

        // Resolve DNS and verify the IP is not in a private/reserved range.
        // This catches numeric IPs (e.g. http://192.168.1.1) as well as hostname aliases.
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            // Unresolvable host — reject defensively.
            throw new DomainException(fieldName + ": no se puede resolver el host de la URL.");
        }

        for (InetAddress addr : addresses) {
            if (isPrivateOrReserved(addr)) {
                log.warn("[SECURITY] SSRF attempt blocked: host={} resolvedTo={} field={}",
                        host, addr.getHostAddress(), fieldName);
                throw new DomainException(fieldName + ": URL no permitida.");
            }
        }

        return trimmed;
    }

    /**
     * Returns true for any IP that should not be reachable from user-supplied URLs.
     */
    private static boolean isPrivateOrReserved(InetAddress addr) {
        if (addr.isLoopbackAddress()) return true;      // 127.x.x.x / ::1
        if (addr.isSiteLocalAddress()) return true;     // 10.x, 172.16-31.x, 192.168.x
        if (addr.isLinkLocalAddress()) return true;     // 169.254.x.x (cloud metadata)
        if (addr.isAnyLocalAddress()) return true;      // 0.0.0.0
        if (addr.isMulticastAddress()) return true;     // 224.x - 239.x

        // Carrier-grade NAT: 100.64.0.0/10 (RFC 6598) — used by some cloud providers
        // for internal traffic.
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            if (first == 100 && second >= 64 && second <= 127) return true;
        }

        return false;
    }
}
