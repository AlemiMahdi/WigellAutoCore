package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testar att GarageSystem skapar utkast med automatisk bokning (WAC-63).
// Database innehåller exempeldata: fordon 1-3 med kunder 1-3.
public class GarageSystemTest {

    private final GarageSystem garageSystem = new GarageSystem();

    // Ett giltigt utkast ska få status DRAFT och en automatisk bokning
    // med fordon och beskrivning, men utan datum.
    @Test
    void createDraftWorkOrderCreateDraftAndBooking() {
        WorkOrder draft = garageSystem.createDraftWorkOrder(
                1, " Konstigt ljud från bromsarna ", WorkOrderTypeEnum.DROP_IN);

        assertEquals(WorkOrderStatus.DRAFT, draft.getStatus());
        assertEquals(WorkOrderTypeEnum.DROP_IN, draft.getType());

        Booking booking = findBooking(draft.getBookingId());
        assertNotNull(booking);
        assertEquals(1, booking.getVehicleId());
        assertEquals("Konstigt ljud från bromsarna", booking.getDescription());
        assertEquals("WORK_ORDER_CREATED", booking.getStatus());
        assertNull(booking.getDate());
    }

    // Okänt fordon ska stoppas, och inget ska läggas till i listorna.
    @Test
    void createDraftWorkOrderWithUnknownVehicleThrow() {
        int bookingsBefore = Database.getBookings().size();
        int workOrdersBefore = Database.getWorkOrders().size();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> garageSystem.createDraftWorkOrder(999, "Ljud", WorkOrderTypeEnum.PLANNED));

        assertEquals("createDraft.vehicleMissing", exception.getMessage());
        assertEquals(bookingsBefore, Database.getBookings().size());
        assertEquals(workOrdersBefore, Database.getWorkOrders().size());
    }

    // En beskrivning med bara mellanslag räknas som tom.
    @Test
    void createDraftWorkOrderWithBlankDescriptionThrow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> garageSystem.createDraftWorkOrder(1, " ", WorkOrderTypeEnum.PLANNED));

        assertEquals("createDraft.descriptionMissing", exception.getMessage());
    }

    // Saknas typ ska ingen bokning bli kvar utan arbetsorder.
    @Test
    void createDraftWorkOrderWithoutTypeLeavesNoBooking() {
        int bookingsBefore = Database.getBookings().size();

        assertThrows(IllegalArgumentException.class,
                () -> garageSystem.createDraftWorkOrder(1, "Ljud", null));

        assertEquals(bookingsBefore, Database.getBookings().size());
    }

    // Hjälpmetod: hittar en bokning i listan med id.
    private Booking findBooking(int id) {
        for (Booking booking: Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }
}
