
package com.wac.autocore.security.actions;

import com.wac.autocore.service.WorkOrderService;

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
    classes = SecuredWorkOrderActionsSecurityTest.TestConfig.class
)
class SecuredWorkOrderActionsSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        WorkOrderService workOrderService() {
            return mock(WorkOrderService.class);
        }

        @Bean
        SecuredWorkOrderActions securedWorkOrderActions(
                WorkOrderService workOrderService) {
            return new SecuredWorkOrderActions(workOrderService);
        }
    }

    @Autowired
    private SecuredWorkOrderActions securedWorkOrderActions;

    @Autowired
    private WorkOrderService workOrderService;

    private void loginAs(String role) {
        var authentication =
                new UsernamePasswordAuthenticationToken(
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
        reset(workOrderService);
    }

    @Test
    void adminCanStartWorkOrder() {
        loginAs("ADMIN");

        securedWorkOrderActions.startWorkOrder(1);

        verify(workOrderService).startWorkOrder(1);
    }

    @Test
    void workshopCanCompleteWorkOrder() {
        loginAs("WORKSHOP");

        securedWorkOrderActions.completeWorkOrder(1);

        verify(workOrderService).completeWorkOrder(1);
    }

    @Test
    void readOnlyCannotStartWorkOrder() {
        loginAs("READ_ONLY");

        assertThrows(
            AccessDeniedException.class,
            () -> securedWorkOrderActions.startWorkOrder(1)
        );

        verifyNoInteractions(workOrderService);
    }

    @Test
    void readOnlyCannotCompleteWorkOrder() {
        loginAs("READ_ONLY");

        assertThrows(
            AccessDeniedException.class,
            () -> securedWorkOrderActions.completeWorkOrder(1)
        );

        verifyNoInteractions(workOrderService);
    }

    @Test
    void allRolesCanViewWorkOrders() {
        for (String role : List.of(
                "ADMIN", "WORKSHOP", "READ_ONLY")) {

            loginAs(role);
            securedWorkOrderActions.findAll();
        }

        verify(workOrderService, times(3)).findAll();
    }
}
