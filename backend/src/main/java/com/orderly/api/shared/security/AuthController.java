package com.orderly.api.shared.security;

import com.orderly.api.business.application.CreateBusinessCommand;
import com.orderly.api.business.application.CreateBusinessUseCase;
import com.orderly.api.business.domain.port.BusinessRepository;
import com.orderly.api.business.interfaces.rest.BusinessResponse;
import com.orderly.api.product.application.CreateProductCommand;
import com.orderly.api.product.application.ProductCatalogUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authentication endpoints for JWT login and current user inspection.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final InMemoryUserAccountService userAccountService;
    private final JwtService jwtService;
    private final BusinessRepository businessRepository;
    private final CreateBusinessUseCase createBusinessUseCase;
    private final ProductCatalogUseCase productCatalogUseCase;

    public AuthController(
            InMemoryUserAccountService userAccountService,
            JwtService jwtService,
            BusinessRepository businessRepository,
            CreateBusinessUseCase createBusinessUseCase,
            ProductCatalogUseCase productCatalogUseCase) {
        this.userAccountService = userAccountService;
        this.jwtService = jwtService;
        this.businessRepository = businessRepository;
        this.createBusinessUseCase = createBusinessUseCase;
        this.productCatalogUseCase = productCatalogUseCase;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserPrincipal principal = userAccountService.authenticate(request.email(), request.password());
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

        // Create initial catalog products from onboarding wizard
        if (request.products() != null) {
            for (RegisterRequest.InitialProduct p : request.products()) {
                productCatalogUseCase.create(new CreateProductCommand(
                        business.id(), p.name(), p.description(), null, p.price()));
            }
        }

        // Save bot mascot identity if provided during onboarding
        String botName = (request.botName() != null && !request.botName().isBlank())
                ? request.botName()
                : "Orderly";
        String botEmoji = (request.botEmoji() != null && !request.botEmoji().isBlank())
                ? request.botEmoji()
                : "🤖";
        businessRepository.updateBotIdentity(business.id(), botName, botEmoji);

        return toAuthResponse(principal);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return toAuthResponse(principal);
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
