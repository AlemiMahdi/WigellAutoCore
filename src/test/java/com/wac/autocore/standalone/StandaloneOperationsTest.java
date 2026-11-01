package com.wac.autocore.standalone;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.security.AuthenticationService;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tester för de två operationerna i det fristående programmet (WAC-106).
 * Varje operation testas med godkänt och nekat fall,
 * både för affärsregeln och för behörigheten.
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

        // Testerna loggar inte in med lösenord, så AuthenticationManager används aldrig
        AuthenticationManager notUsed = authentication -> {
            throw new UnsupportedOperationException("Not used in tests");
        };
        operations = new StandaloneOperations(bookingService, new AuthenticationService(notUsed));
    }

    // Körs efter varje test, så att inloggningen inte följer med till nästa test
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Sätter en inloggad användare med given roll, som MA gör i sina säkerhetstester
    private void loginAs(Role role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "test-user",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                )
        );
    }

    // ---------- Affärsregler (inloggad som WORKSHOP) ----------

    // Operation 1, godkänt: DRAFT -> CONFIRMED är tillåtet
    @Test
    void changeWorkOrderStatus_allowedChange_returnsTrue() {
        loginAs(Role.WORKSHOP);
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean result = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.CONFIRMED);

        assertTrue(result);
        assertEquals(WorkOrderStatus.CONFIRMED, workOrder.getStatus());
    }

    // Operation 1, nekat: DRAFT -> COMPLETED är inte tillåtet
    @Test
    void changeWorkOrderStatus_deniedChange_returnsFalse() {
        loginAs(Role.WORKSHOP);
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean result = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.COMPLETED);

        assertFalse(result);
        assertEquals(WorkOrderStatus.DRAFT, workOrder.getStatus());
    }

    // Operation 2, godkänt: kl 13:00 krockar inte med 10:00–11:00
    @Test
    void canBook_freeTime_returnsTrue() {
        loginAs(Role.WORKSHOP);

        boolean result = operations.canBook(MECHANIC_ID, DATE, LocalTime.of(13, 0), 60);

        assertTrue(result);
    }

    // Operation 2, nekat: kl 10:30 krockar med 10:00–11:00
    @Test
    void canBook_overlappingTime_returnsFalse() {
        loginAs(Role.WORKSHOP);

        boolean result = operations.canBook(MECHANIC_ID, DATE, LocalTime.of(10, 30), 60);

        assertFalse(result);
    }

    // ---------- Behörighet ----------

    // READ_ONLY får inte byta status, även om bytet i sig är tillåtet
    @Test
    void changeWorkOrderStatus_readOnly_isDenied() {
        loginAs(Role.READ_ONLY);
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        boolean result = operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.CONFIRMED);

        assertFalse(result);
        assertEquals(WorkOrderStatus.DRAFT, workOrder.getStatus());
    }

    // READ_ONLY får inte boka, även om tiden är ledig
    @Test
    void canBook_readOnly_isDenied() {
        loginAs(Role.READ_ONLY);

        boolean result = operations.canBook(MECHANIC_ID, DATE, LocalTime.of(13, 0), 60);

        assertFalse(result);
    }

    // Ingen inloggad användare nekas båda operationerna
    @Test
    void operations_notLoggedIn_areDenied() {
        WorkOrder workOrder = WorkOrder.createDraft(1, 1, WorkOrderTypeEnum.PLANNED);

        assertFalse(operations.changeWorkOrderStatus(workOrder, WorkOrderStatus.CONFIRMED));
        assertFalse(operations.canBook(MECHANIC_ID, DATE, LocalTime.of(13, 0), 60));
    }
}