package com.orderly.api.email.application;

import com.orderly.api.email.domain.port.EmailService;
import com.orderly.api.email.infrastructure.config.EmailProperties;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Map;

@Service
public class UserEmailService {

    private final EmailService emailService;
    private final EmailTemplateRenderer renderer;
    private final EmailProperties props;

    public UserEmailService(EmailService emailService, EmailTemplateRenderer renderer, EmailProperties props) {
        this.emailService = emailService;
        this.renderer = renderer;
        this.props = props;
    }

    public void sendVerificationEmail(String to, String fullName, String token) {
        String link = props.getAppBaseUrl() + "/verify-email?token=" + token;
        String html = renderer.render("email-verification", Map.of(
                "NAME", firstName(fullName),
                "LINK", link,
                "YEAR", currentYear()
        ));
        emailService.send(to, "Verifica tu correo electrónico — Orderly", html);
    }

    public void sendWelcomeEmail(String to, String fullName, String businessName) {
        String html = renderer.render("welcome", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "DASHBOARD_LINK", props.getAppBaseUrl() + "/dashboard",
                "YEAR", currentYear()
        ));
        emailService.send(to, "¡Bienvenido a Orderly, " + firstName(fullName) + "! 🎉", html);
    }

    public void sendOnboardingReminderEmail(String to, String fullName, String businessName, int step) {
        String stepLabel = resolveOnboardingStep(step);
        String html = renderer.render("onboarding-reminder", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "STEP_LABEL", stepLabel,
                "SETUP_LINK", props.getAppBaseUrl() + "/settings",
                "YEAR", currentYear()
        ));
        emailService.send(to, "Tu negocio " + businessName + " está casi listo — paso pendiente", html);
    }

    public void sendInactivityReminderEmail(String to, String fullName, String businessName) {
        String html = renderer.render("inactivity-reminder", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "DASHBOARD_LINK", props.getAppBaseUrl() + "/dashboard",
                "YEAR", currentYear()
        ));
        emailService.send(to, "Te extrañamos — ¿todo bien con " + businessName + "?", html);
    }

    public void sendPasswordResetEmail(String to, String fullName, String token) {
        String link = props.getAppBaseUrl() + "/reset-password?token=" + token;
        String html = renderer.render("password-reset", Map.of(
                "NAME", firstName(fullName),
                "LINK", link,
                "YEAR", currentYear()
        ));
        emailService.send(to, "Recuperación de contraseña — Orderly", html);
    }

    public void sendPasswordChangedEmail(String to, String fullName) {
        String html = renderer.render("password-changed", Map.of(
                "NAME", firstName(fullName),
                "SUPPORT_EMAIL", props.getFromAddress(),
                "YEAR", currentYear()
        ));
        emailService.send(to, "Tu contraseña fue actualizada — Orderly", html);
    }

    public void sendTrialExpiryEmail(String to, String fullName, String businessName, int daysLeft) {
        String html = renderer.render("trial-expiry", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "DAYS_LEFT", String.valueOf(daysLeft),
                "PRICING_LINK", props.getAppBaseUrl() + "/pricing",
                "YEAR", currentYear()
        ));
        emailService.send(to, "Tu periodo de prueba vence en " + daysLeft + " días — activa tu plan", html);
    }

    public void sendPaymentReminderEmail(String to, String fullName, String businessName, String planName, String renewalDate) {
        String html = renderer.render("payment-reminder", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "PLAN_NAME", planName,
                "RENEWAL_DATE", renewalDate,
                "BILLING_LINK", props.getAppBaseUrl() + "/billing",
                "YEAR", currentYear()
        ));
        emailService.send(to, "Recordatorio de renovación — plan " + planName, html);
    }

    public void sendUsageWarningEmail(String to, String fullName, String businessName, int percentUsed) {
        String html = renderer.render("usage-warning", Map.of(
                "NAME", firstName(fullName),
                "BUSINESS_NAME", businessName,
                "PERCENT_USED", String.valueOf(percentUsed),
                "UPGRADE_LINK", props.getAppBaseUrl() + "/billing",
                "YEAR", currentYear()
        ));
        emailService.send(to, "Alerta de uso — " + percentUsed + "% de tu plan consumido", html);
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "amigo";
        return fullName.trim().split("\\s+")[0];
    }

    private String currentYear() {
        return String.valueOf(Year.now().getValue());
    }

    private String resolveOnboardingStep(int step) {
        return switch (step) {
            case 0 -> "Conectar tu número de WhatsApp";
            case 1 -> "Configurar los horarios de atención";
            case 2 -> "Agregar productos a tu catálogo";
            case 3 -> "Activar tu chatbot";
            default -> "Completar la configuración inicial";
        };
    }
}
