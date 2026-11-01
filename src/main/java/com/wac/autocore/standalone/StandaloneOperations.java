package com.wac.autocore.standalone;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.service.BookingService;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * De två verksamhetsoperationerna i det fristående programmet (WAC-106).
 * Använder samma affärsregler som appen: BookingService och WorkOrder.
 * Ingen Spring här, BookingService skickas in via konstruktorn.
 */
public class StandaloneOperations {

    private final BookingService bookingService;

    // Dependency Injection: service skickas in, klassen skapar den inte själv
    public StandaloneOperations(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Operation 1: byter status på arbetsordern om regeln i WorkOrderStatus tillåter det.
    // Returnerar true om bytet godkändes, false om det nekades.
    public boolean changeWorkOrderStatus(WorkOrder workOrder, WorkOrderStatus newStatus) {
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
        return !bookingService.hasOverlappingBooking(mechanicId, date, startTime, durationMinutes);
    }
}
