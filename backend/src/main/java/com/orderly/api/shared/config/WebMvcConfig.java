package com.orderly.api.shared.config;

import com.orderly.api.shared.security.AuthRateLimitInterceptor;
import com.orderly.api.shared.security.WebhookRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC interceptor registration.
 *
 * Rate limiters registered here:
 *  - AuthRateLimitInterceptor    → /api/v1/auth/login + /register  (brute-force protection)
 *  - WebhookRateLimitInterceptor → /webhook/whatsapp/**             (DoS / order-flood protection)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthRateLimitInterceptor authRateLimitInterceptor;
    private final WebhookRateLimitInterceptor webhookRateLimitInterceptor;

    public WebMvcConfig(
            AuthRateLimitInterceptor authRateLimitInterceptor,
            WebhookRateLimitInterceptor webhookRateLimitInterceptor) {
        this.authRateLimitInterceptor = authRateLimitInterceptor;
        this.webhookRateLimitInterceptor = webhookRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(authRateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/resend-verification",
                        "/api/v1/auth/verify-email"
                );

        registry.addInterceptor(webhookRateLimitInterceptor)
                .addPathPatterns("/webhook/whatsapp/**");
    }
}
