package com.wac.autocore.standalone;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.security.AuthenticationService;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * De två verksamhetsoperationerna i det fristående programmet (WAC-106).
 * Använder samma affärsregler som appen: BookingService och WorkOrder.
 * Ingen Spring här, BookingService och AuthenticationService skickas in via konstruktorn.
 * Bara ADMIN och WORKSHOP får köra operationerna, samma roller som i appen.
 */
public class StandaloneOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneOperations.class);

    private final BookingService bookingService;
    private final AuthenticationService authService;

    // Dependency Injection: service skickas in, klassen skapar den inte själv
    public StandaloneOperations(BookingService bookingService, AuthenticationService authService) {
        this.bookingService = bookingService;
        this.authService = authService;
    }

    // Operation 1: byter status på arbetsordern om regeln i WorkOrderStatus tillåter det.
    // Returnerar true om bytet godkändes, false om det nekades.
    public boolean changeWorkOrderStatus(WorkOrder workOrder, WorkOrderStatus newStatus) {
        if (!canModifyData()) {
            return false;
        }
        try {
            workOrder.changeStatus(newStatus);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    // Operation 2: kontrollerar om mekanikern är ledig vid önskad tid.
    // Returnerar true om bokningen kan göras, false om den krockar.
    public boolean canBook(int mechanicId, LocalDate date, LocalTime startTime, int durationMinutes) {
        if (!canModifyData()) {
            return false;
        }
        return !bookingService.hasOverlappingBooking(mechanicId, date, startTime, durationMinutes);
    }

    // Samma roller som @PreAuthorize("hasAnyRole('ADMIN', 'WORKSHOP')") i appen.
    private boolean canModifyData() {
        if (authService.hasRole(Role.ADMIN) || authService.hasRole(Role.WORKSHOP)) {
            return true;
        }
        LOGGER.warn("Access denied for user {}", authService.getCurrentUsername());
        return false;
    }
}
