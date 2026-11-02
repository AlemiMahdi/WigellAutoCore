
package com.wac.autocore.security.actions;

import com.wac.autocore.service.BookingService;

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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(
    classes = SecuredBookingActionsSecurityTest.TestConfig.class
)
class SecuredBookingActionsSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        BookingService bookingService() {
            return mock(BookingService.class);
        }

        @Bean
        SecuredBookingActions securedBookingActions(
                BookingService bookingService) {
            return new SecuredBookingActions(bookingService);
        }
    }

    @Autowired
    private SecuredBookingActions securedBookingActions;

    @Autowired
    private BookingService bookingService;

    private void loginAs(String role) {
        var authentication =
                new UsernamePasswordAuthenticationToken(
                    "testUser",
                    null,
                    List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                    )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        reset(bookingService);
    }

    @Test
    void adminCanUpdateBookingServices() {
        loginAs("ADMIN");

        securedBookingActions.updateBookingServices(
                1, List.of(2, 3));

        verify(bookingService).updateBookingServices(
                1, List.of(2, 3));
    }

    @Test
    void workshopCanUpdateBookingServices() {
        loginAs("WORKSHOP");

        securedBookingActions.updateBookingServices(
                1, List.of(2));

        verify(bookingService).updateBookingServices(
                1, List.of(2));
    }

    @Test
    void readOnlyCannotUpdateBookingServices() {
        loginAs("READ_ONLY");

        assertThrows(
            AccessDeniedException.class,
            () -> securedBookingActions.updateBookingServices(
                    1, List.of(2))
        );

        verifyNoInteractions(bookingService);
    }

    @Test
    void allRolesCanViewBookings() {
        for (String role : List.of(
                "ADMIN", "WORKSHOP", "READ_ONLY")) {

            loginAs(role);
            securedBookingActions.getAllBookings();
        }

        verify(bookingService, times(3)).getAllBookings();
    }

    @Test
    void readOnlyCannotCheckBookingOverlap() {
        loginAs("READ_ONLY");

        assertThrows(
            AccessDeniedException.class,
            () -> securedBookingActions.hasOverlappingBooking(
                    1,
                    java.time.LocalDate.of(2026, 11, 10),
                    java.time.LocalTime.of(10, 0),
                    60
            )
        );

        verifyNoInteractions(bookingService);
    }

    @Test
    void unauthenticatedCannotViewBookings() {
        SecurityContextHolder.clearContext();

        assertThrows(
            AuthenticationCredentialsNotFoundException.class,
            () -> securedBookingActions.getAllBookings()
        );

        verifyNoInteractions(bookingService);
    }
}
