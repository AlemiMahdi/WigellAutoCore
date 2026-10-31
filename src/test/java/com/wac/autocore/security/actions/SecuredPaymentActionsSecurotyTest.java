
package com.wac.autocore.security.actions;

import com.wac.autocore.service.PaymentService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(
    classes = SecuredPaymentActionsSecurityTest.TestConfig.class
)
class SecuredPaymentActionsSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        PaymentService paymentService() {
            return mock(PaymentService.class);
        }

        @Bean
        SecuredPaymentActions securedPaymentActions(
                PaymentService paymentService) {

            return new SecuredPaymentActions(paymentService);
        }
    }

    @Autowired
    private SecuredPaymentActions securedPaymentActions;

    @Autowired
    private PaymentService paymentService;

    // Simulerar en inloggad användare med en viss roll.
    private void loginAs(String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        reset(paymentService);
    }

    @Test
    void adminCanProcessPayment() {
        loginAs("ADMIN");

        securedPaymentActions.processPayment(null, "CARD");

        verify(paymentService).processPayment(null, "CARD");
    }

    @Test
    void workshopCannotProcessPayment() {
        loginAs("WORKSHOP");

        assertThrows(
                AccessDeniedException.class,
                () -> securedPaymentActions.processPayment(null, "CARD")
        );

        verifyNoInteractions(paymentService);
    }

    @Test
    void readOnlyCannotProcessPayment() {
        loginAs("READ_ONLY");

        assertThrows(
                AccessDeniedException.class,
                () -> securedPaymentActions.processPayment(null, "CARD")
        );

        verifyNoInteractions(paymentService);
    }

    @Test
    void allRolesCanViewPayments() {
        for (String role : List.of(
                "ADMIN", "WORKSHOP", "READ_ONLY")) {

            loginAs(role);
            securedPaymentActions.findAll();
        }

        verify(paymentService, times(3)).findAll();
    }
}
