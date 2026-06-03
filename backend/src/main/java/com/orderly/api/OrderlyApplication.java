package com.orderly.api;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.orderly.api.business.infrastructure.config.BusinessDefaultsProperties;
import com.orderly.api.plan.infrastructure.stripe.StripeProperties;
import com.orderly.api.shared.config.CorsProperties;
import com.orderly.api.shared.config.EvolutionProperties;
import com.orderly.api.shared.security.InMemoryUserAccountService;
import com.orderly.api.shared.security.JwtProperties;

/**
 * Main entry point for the ORDERLY backend API.
 */
@SpringBootApplication
@EnableConfigurationProperties({
        BusinessDefaultsProperties.class,
        JwtProperties.class,
        EvolutionProperties.class,
        CorsProperties.class,
        StripeProperties.class
})
public class OrderlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderlyApplication.class, args);
    }

    @Bean
    ApplicationRunner seedUsers(InMemoryUserAccountService userService) {
        return args -> userService.seedDefaults();
    }
}
