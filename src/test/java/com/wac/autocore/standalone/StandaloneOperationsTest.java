package com.wac.autocore.standalone;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tester för de två operationerna i det fristående programmet (WAC-106).
 * Varje operation testas med ett godkänt och ett nekat fall.
 */
class StandaloneOperationsTest {

    private static final int MECHANIC_ID = 1;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 10);

    private StandaloneOperations operations;

    // Körs före varje test, så att varje test börjar med samma data
    @BeforeEach
    void setUp() {
        InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();

        // En befintlig bokning: mekaniker 1, kl 10:00–11:00
        Booking existing = new Booking(1, DATE, LocalTime.of(10, 0), 60, "Oil change");
        bookingRepository.save(existing, MECHANIC_ID);

        BookingService bookingService = new BookingService(
                bookingRepository,
                new InMemoryWorkOrderRepository(),
                new InMemoryServiceItemRepository()
        );
        operations = new StandaloneOperations(bookingService);
    }

    // Operation 1, godkänt: DRAFT -> CONFIRMED är tillåtet
    @Test
    void changeWorkOrderStatus_allowedChange_returnsTrue() {
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean result = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.CONFIRMED);

        assertTrue(result);
        assertEquals(WorkOrderStatus.CONFIRMED, workOrder.getStatus());
    }

    // Operation 1, nekat: DRAFT -> COMPLETED är inte tillåtet
    @Test
    void changeWorkOrderStatus_deniedChange_returnsFalse() {
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean result = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.COMPLETED);

        assertFalse(result);
        assertEquals(WorkOrderStatus.DRAFT, workOrder.getStatus());
    }

    // Operation 2, godkänt: kl 13:00 krockar inte med 10:00–11:00
    @Test
    void canBook_freeTime_returnsTrue() {
        boolean result = operations.canBook(MECHANIC_ID, DATE, LocalTime.of(13, 0), 60);

        assertTrue(result);
    }

    // Operation 2, nekat: kl 10:30 krockar med 10:00–11:00
    @Test
    void canBook_overlappingTime_returnsFalse() {
        boolean result = operations.canBook(MECHANIC_ID, DATE, LocalTime.of(10, 30), 60);

        assertFalse(result);
    }
}