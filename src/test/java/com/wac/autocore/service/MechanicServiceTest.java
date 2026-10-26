package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.IMechanicRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MechanicServiceTest {

    private List<Booking> originalBookings;
    private List<WorkOrder> originalOrders;

    @BeforeEach
    void preserveData() {
        originalBookings = new ArrayList<>(Database.getBookings());
        originalOrders = new ArrayList<>(Database.getWorkOrders());

        Database.getBookings().clear();
        Database.getWorkOrders().clear();
    }

    @AfterEach
    void restoreData() {
        Database.getBookings().clear();
        Database.getBookings().addAll(originalBookings);

        Database.getWorkOrders().clear();
        Database.getWorkOrders().addAll(originalOrders);
    }

    @Test
    void readsMechanicsThroughRepository() {
        FakeMechanicRepository repository = new FakeMechanicRepository();
        MechanicService service = new MechanicService(repository);

        List<Mechanic> mechanics = service.findAll();

        assertNotNull(mechanics);
        assertTrue(mechanics.isEmpty());
        assertEquals(1, repository.findAllCalls);
    }

    @Test
    void propagatesStorageFailure() {
        FakeMechanicRepository repository = new FakeMechanicRepository();
        RuntimeException failure =
                new IllegalStateException("Storage unavailable");
        repository.readFailure = failure;

        MechanicService service = new MechanicService(repository);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.findAll()
        );

        assertSame(failure, thrown);
    }

    @Test
    void scheduleFiltersMechanicAndSortsByDateAndTime() {
        LocalDate date = LocalDate.of(2026, 10, 20);

        Booking morning = new Booking(
                1, 1, date, LocalTime.of(9, 0), 60, "Morning"
        );
        Booking afternoon = new Booking(
                  2, 1, date, LocalTime.of(14, 0), 60, "Afternoon"
        );
        Booking nextDay = new Booking(
                 3, 1, date.plusDays(1), LocalTime.of(8, 0), 60, "Next day"
        );
        Booking withoutDate = new Booking(4, 1, null, "Unscheduled");
        Booking otherMechanic = new Booking(5, 1, date, "Other mechanic");

        morning.setMechanicId(1);
        afternoon.setMechanicId(1);
        nextDay.setMechanicId(1);
        withoutDate.setMechanicId(1);
        otherMechanic.setMechanicId(2);

        List<Booking> input = Arrays.asList(
                withoutDate, nextDay, afternoon, otherMechanic, morning
        );
        Database.getBookings().addAll(input);

        MechanicService service = new MechanicService(
                new FakeMechanicRepository(),
                () -> new ArrayList<>(input),
                () -> new ArrayList<WorkOrder>()
        );

        List<Booking> result = service.getSchedule(1);

        assertEquals(
                Arrays.asList(morning, afternoon, nextDay, withoutDate),
                result
        );

        // Hämtningen ska inte ändra originalets ordning.
        assertEquals(input, Database.getBookings());
    }

    @Test
    void workOrderMechanicTakesPriorityOverBookingMechanic() {
        Booking booking = new Booking(
                1, 1, LocalDate.of(2026, 10, 20), "Test"
        );
        booking.setMechanicId(1);
        Database.getBookings().add(booking);

        WorkOrder order = new WorkOrder(booking.getId(), 2, WorkOrderTypeEnum.PLANNED
        );
        Database.getWorkOrders().add(order);

        MechanicService service = new MechanicService(
                new FakeMechanicRepository(),
                () -> Arrays.asList(booking),
                () -> Arrays.asList(order)
        );

        assertTrue(service.getSchedule(1).isEmpty());
        assertEquals(Arrays.asList(booking), service.getSchedule(2));

        // Schemat får inte skriva över bokningens uppgifter.
        assertEquals(Integer.valueOf(1), booking.getMechanicId());
    }

    @Test
    void schedulePropagatesBookingLoadFailure() {
        Booking booking = new Booking(
                1, 1, LocalDate.of(2026, 10, 20), "Existing"
        );
        Database.getBookings().add(booking);
        List<Booking> before = new ArrayList<>(Database.getBookings());

        RuntimeException failure =
                new IllegalStateException("Booking storage unavailable");

        MechanicService service = new MechanicService(
                new FakeMechanicRepository(),
                () -> {
                    throw failure;
                },
                () -> new ArrayList<WorkOrder>()
        );

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.getSchedule(1)
        );

        assertSame(failure, thrown);
        assertEquals(before, Database.getBookings());
    }

    private static class FakeMechanicRepository
            implements IMechanicRepository {

        private int findAllCalls;
        private RuntimeException readFailure;

        @Override
        public Mechanic save(Mechanic mechanic) {
            throw new UnsupportedOperationException(
                    "Saving is not used in these tests"
            );
        }

        @Override
        public List<Mechanic> findAll() {
            findAllCalls++;
            if (readFailure != null) {
                throw readFailure;
            }
            return new ArrayList<>();
        }

    }
}