package com.orderly.api.shared.security;

import com.orderly.api.business.application.CreateBusinessCommand;
import com.orderly.api.business.application.CreateBusinessUseCase;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.business.interfaces.rest.BusinessResponse;
import com.orderly.api.email.application.UserEmailService;
import com.orderly.api.product.application.CreateProductCommand;
import com.orderly.api.product.application.ProductCatalogUseCase;
import com.orderly.api.shared.security.persistence.UserJpaEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final InMemoryUserAccountService userAccountService;
    private final JwtService jwtService;
    private final BusinessRepository businessRepository;
    private final CreateBusinessUseCase createBusinessUseCase;
    private final ProductCatalogUseCase productCatalogUseCase;
    private final UserEmailService userEmailService;

    public AuthController(
            InMemoryUserAccountService userAccountService,
            JwtService jwtService,
            BusinessRepository businessRepository,
            CreateBusinessUseCase createBusinessUseCase,
            ProductCatalogUseCase productCatalogUseCase,
            UserEmailService userEmailService) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.businessRepository = businessRepository;
        this.createBusinessUseCase = createBusinessUseCase;
        this.productCatalogUseCase = productCatalogUseCase;
        this.userEmailService = userEmailService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserPrincipal principal = userAccountService.authenticate(request.email(), request.password());
        userAccountService.recordLogin(request.email());
        return toAuthResponse(principal);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        UserPrincipal principal = userAccountService.registerNew(
                request.email(),
                request.fullName(),
                request.password(),
                "ADMIN");

        var business = createBusinessUseCase.create(new CreateBusinessCommand(
                principal.userId(),
                request.businessName(),
                request.businessType(),
                request.countryCode() != null ? request.countryCode() : "CO",
                request.currencyCode() != null ? request.currencyCode() : "COP",
                request.timezone() != null ? request.timezone() : "America/Bogota"));

        if (request.products() != null) {
            for (RegisterRequest.InitialProduct p : request.products()) {
                productCatalogUseCase.create(new CreateProductCommand(
                        business.id(), p.name(), p.description(), null, p.price()));
            }
        }

        String botName = (request.botName() != null && !request.botName().isBlank())
                ? request.botName()
                : "Orderly";
        String botEmoji = (request.botEmoji() != null && !request.botEmoji().isBlank())
                ? request.botEmoji()
                : "🤖";
        businessRepository.updateBotIdentity(business.id(), botName, botEmoji);

        // Send verification email asynchronously
        String token = userAccountService.getVerificationToken(request.email());
        if (token != null) {
            userEmailService.sendVerificationEmail(request.email(), request.fullName(), token);
        }

        return toAuthResponse(principal);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return toAuthResponse(principal);
    }

    @PostMapping("/verify-email")
    public Map<String, String> verifyEmail(@RequestParam String token) {
        userAccountService.verifyEmail(token);
        return Map.of("message", "Correo verificado exitosamente.");
    }

    @PostMapping("/resend-verification")
    public Map<String, String> resendVerification(@AuthenticationPrincipal UserPrincipal principal) {
        UserJpaEntity user = userAccountService.generateNewVerificationToken(principal.getUsername());
        userEmailService.sendVerificationEmail(user.getEmail(), user.getFullName(), user.getEmailVerificationToken());
        return Map.of("message", "Correo de verificación reenviado.");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            UserJpaEntity user = userAccountService.createPasswordResetToken(request.email());
            userEmailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), user.getPasswordResetToken());
        } catch (Exception ignored) {
            // Always return 200 to prevent email enumeration
        }
        return Map.of("message", "Si el correo existe, recibirás un enlace de recuperación en los próximos minutos.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(request.token(), request.newPassword());
        return Map.of("message", "Contraseña actualizada exitosamente. Inicia sesión con tu nueva contraseña.");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        UserJpaEntity user = userAccountService.changePassword(
                principal.userId(), request.currentPassword(), request.newPassword());
        userEmailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
        return Map.of("message", "Contraseña actualizada exitosamente.");
    }

    private AuthResponse toAuthResponse(UserPrincipal principal) {
        List<BusinessResponse> businesses = businessRepository.findAllByOwnerId(principal.userId())
                .stream()
                .map(BusinessResponse::from)
                .toList();

        return new AuthResponse(
                jwtService.createToken(principal),
                new AuthResponse.AuthUserResponse(
                        principal.userId().toString(),
                        principal.fullName(),
                        principal.getUsername(),
                        principal.role()),
                businesses);
    }
}
