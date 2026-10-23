package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WorkOrderServiceTest {
    private final FakeWorkOrderRepository fakeRepository = new FakeWorkOrderRepository();
    private final FakeVehicleRepository fakeVehicleRepository = new FakeVehicleRepository();
    private final FakeMechanicRepository fakeMechanicRepository = new FakeMechanicRepository();
    private final WorkOrderService workOrderService = new WorkOrderService(fakeRepository, null, null, fakeVehicleRepository, fakeMechanicRepository);

    @Test
    void createDraftSavesDraftAndBooking() {
        WorkOrder draft = workOrderService.createNewDraftWorkOrder(
                1, "Bromsljus", WorkOrderTypeEnum.PLANNED, null);

        try {
            assertEquals(WorkOrderStatus.DRAFT, draft.getStatus());
            assertNotNull(fakeRepository.saveDraft);
            assertEquals(1, fakeRepository.saveBooking.getVehicleId());
        } finally {
            Database.getWorkOrders().remove(draft);
            Database.getBookings().remove(fakeRepository.saveBooking);
        }
    }

    @Test
    void complaintWorkOrderIsLinkedToOriginalWorkOrder() {
        int originalWorkOrderId = 42;

        // Kunden kommer tillbaka
        WorkOrder complaint = workOrderService.createNewDraftWorkOrder(
                1, "Bromsarna låter fortfarande", WorkOrderTypeEnum.COMPLAINT, originalWorkOrderId);

        try {
            assertEquals(WorkOrderTypeEnum.COMPLAINT, complaint.getType());
            assertEquals(originalWorkOrderId, complaint.getOriginalWorkOrderId());
            assertSame(complaint, fakeRepository.saveDraft);
        } finally {
            Database.getWorkOrders().remove(complaint);
            Database.getBookings().remove(fakeRepository.saveBooking);
        }
    }

    @Test
    void dropInWorkOrderCanBeCreatedWithoutPreviousBooking() {
        int bookingsBefore = Database.getBookings().size();

        // Kunden kommer in utan bokning
        WorkOrder draft = workOrderService.createNewDraftWorkOrder(
                1, "Konstigt ljud från bromsarna", WorkOrderTypeEnum.DROP_IN, null);

        try {
            assertEquals(WorkOrderTypeEnum.DROP_IN, draft.getType());
            assertEquals(WorkOrderStatus.DRAFT, draft.getStatus());
            assertNull(draft.getOriginalWorkOrderId());

            // Systemet skapar en egen bokning utan datum
            Booking booking = fakeRepository.saveBooking;
            assertNotNull(booking);
            assertNull(booking.getDate());
            assertEquals("WORK_ORDER_CREATED", booking.getStatus());
            assertEquals(bookingsBefore + 1, Database.getBookings().size());
        } finally {
            Database.getWorkOrders().remove(draft);
            Database.getBookings().remove(fakeRepository.saveBooking);
        }
    }

    @Test
    void complaintCannotStartWithoutOriginalWorkOrder() {
        WorkOrder complaint = confirmedComplaint(9001, null);
        Database.getWorkOrders().add(complaint);

        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9001));
            assertEquals("confirmWorkOrder.originalRequired", exception.getMessage());
            assertEquals(WorkOrderStatus.CONFIRMED, complaint.getStatus());
        } finally {
            Database.getWorkOrders().remove(complaint);
        }
    }

    @Test
    void complaintCannotReferenceItself() {
        WorkOrder complaint = confirmedComplaint(9001, 9001);
        Database.getWorkOrders().add(complaint);

        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9001));
            assertEquals("confirmWorkOrder.originalSelfReference", exception.getMessage());
            assertEquals(WorkOrderStatus.CONFIRMED, complaint.getStatus());
        } finally {
            Database.getWorkOrders().remove(complaint);
        }
    }

    @Test
    void complaintCannotStartWhenOriginalWorkOrderIsMissing() {
        // 9999 finns inte i Database
        WorkOrder complaint = confirmedComplaint(9001, 9999);
        Database.getWorkOrders().add(complaint);

        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9001));
            assertEquals("confirmWorkOrder.originalMissing", exception.getMessage());
            assertEquals(WorkOrderStatus.CONFIRMED, complaint.getStatus());
        } finally {
            Database.getWorkOrders().remove(complaint);
        }
    }

    @Test
    void complaintCannotStartWhenOriginalWorkOrderIsNotCompleted() {
        // Originalet är bara bekräftat, inte slutfört
        WorkOrder original = new WorkOrder(1, 1, WorkOrderTypeEnum.PLANNED);
        original.setId(9002);
        WorkOrder complaint = confirmedComplaint(9001, 9002);
        Database.getWorkOrders().add(original);
        Database.getWorkOrders().add(complaint);

        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9001));
            assertEquals("confirmWorkOrder.originalNotCompleted", exception.getMessage());
            assertEquals(WorkOrderStatus.CONFIRMED, complaint.getStatus());
        } finally {
            Database.getWorkOrders().remove(complaint);
            Database.getWorkOrders().remove(original);
        }
    }

    // Bekräftad reklamation (konstruktorn sätter CONFIRMED).
    // originalId sätts före id, annars stoppar setOriginalWorkOrderId självreferensen redan i modellen.
    private WorkOrder confirmedComplaint(int id, Integer originalId) {
        WorkOrder complaint = new WorkOrder(1, 1, WorkOrderTypeEnum.COMPLAINT);
        complaint.setOriginalWorkOrderId(originalId);
        complaint.setId(id);
        return complaint;
    }

    @Test
    void createDraftWithUnknownVehicleThrow() {
        int bookingsBefore = Database.getBookings().size();
        int workOrdersBefore = Database.getWorkOrders().size();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderService.createNewDraftWorkOrder(999, "Bromsljus", WorkOrderTypeEnum.PLANNED, null));

        assertEquals("createDraft.vehicleMissing", exception.getMessage());
        assertEquals(bookingsBefore, Database.getBookings().size());
        assertEquals(workOrdersBefore, Database.getWorkOrders().size());
        assertNull(fakeRepository.saveDraft);
    }

    @Test
    void createDraftWithNoDescriptionThrow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderService.createNewDraftWorkOrder(1, " ", WorkOrderTypeEnum.PLANNED, null));

        assertEquals("createDraft.descriptionMissing", exception.getMessage());
        assertNull(fakeRepository.saveDraft);
    }

    @Test
    void createDraftWorkOrderWithoutTypeThrow() {
        int bookingBefore = Database.getBookings().size();

        assertThrows(IllegalArgumentException.class,
                () -> workOrderService.createNewDraftWorkOrder(1, "Bromsljus", null, null));

        assertEquals(bookingBefore, Database.getBookings().size());
        assertNull(fakeRepository.saveDraft);
    }

    @Test
    void findStartableWorkOrdersReturnsOnlyConfirmed() {
        // Arrange
        WorkOrder confirm = WorkOrder.createDraft(9001, 0, WorkOrderTypeEnum.PLANNED);
        confirm.changeStatus(WorkOrderStatus.CONFIRMED);

        WorkOrder draft = WorkOrder.createDraft(9002, 0, WorkOrderTypeEnum.PLANNED);

        Database.getWorkOrders().add(confirm);
        Database.getWorkOrders().add(draft);

        try {
            // Act
            List<WorkOrder> result = workOrderService.findStartableWorkOrders();

            // Assert
            assertTrue(result.contains(confirm));
            assertFalse(result.contains(draft));
        } finally {
            // Städa så att andra tester inte påverkas
            Database.getWorkOrders().remove(confirm);
            Database.getWorkOrders().remove(draft);
        }
    }

    // Okänt id ska ge ett regelfel med språknyckel,
    // så att vyn kan visa ett begripligt meddelande.
    @Test
    void startWorkOrderWithUnknownIdThrows() {
        // Act + Assert – 99999 finns inte i Database
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> workOrderService.startWorkOrder(99999));

        // Rätt nyckel betyder att vyn visar rätt text
        assertEquals("confirmWorkOrder.orderMissing", exception.getMessage());
    }

    // Statusregeln: ett utkast får inte startas direkt,
    // det måste bekräftas först (DRAFT → CONFIRMED → IN_PROGRESS).
    @Test
    void startWorkOrderWithDraftThrowsCannotStart() {
        // Arrange – createDraft ger alltid status DRAFT
        WorkOrder draft = WorkOrder.createDraft(9101, 0, WorkOrderTypeEnum.PLANNED);
        Database.getWorkOrders().add(draft);

        try {
            // Act – försök starta utkastet
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9101));

            // Assert – rätt fel, och statusen är oförändrad
            assertEquals("startWorkOrder.cannotStart", exception.getMessage());
            assertEquals(WorkOrderStatus.DRAFT, draft.getStatus());
        } finally {
            // Database är static och delas av alla tester → städa alltid
            Database.getWorkOrders().remove(draft);
        }
    }

    // En komplett, bekräftad order ska inte kunna startas om mekanikern är upptagen.
    // Testet går igenom hela kontrollkedjan och visar att ett regelfel
    // stoppar INNAN något ändras eller sparas.
    @Test
    void startWorkOrderWithBusyMechanicThrows() {
        // Arrange – bygg upp allt som validateBeforeStart kontrollerar.
        // Id 9201 krockar inte med exempeldatan.
        Customer customer = new Customer(9201, "Testkund", "0700000000", "test@test.se");

        Vehicle vehicle = new Vehicle("TST999", "Volvo", "V70", 2015, 9201);   // ägs av kund 9201
        vehicle.setId(9201);

        Booking booking = new Booking(9201, 9201, LocalDate.now(), "Testbokning");   // för fordon 9201

        Mechanic mechanic = new Mechanic(9201, "Testmekaniker", "0700000000", "Allmän");
        mechanic.setAvailable(false);   // ← det testet handlar om: mekanikern är upptagen

        ServiceItem service = new ServiceItem("Testtjänst", "Bara för test", 100.0, 30);
        service.setId(9201);   // ingen databas i testet, så vi sätter id själva

        // Konstruktorn (inte createDraft) ger CONFIRMED, alltså startbar enligt statusregeln
        WorkOrder workOrder = new WorkOrder(9201, 9201, WorkOrderTypeEnum.PLANNED);   // bokning 9201, mekaniker 9201
        workOrder.setId(9201);
        workOrder.addServiceItem(9201);

        // Lägg allt i minnet, där servicen letar
        Database.getCustomers().add(customer);
        Database.getVehicles().add(vehicle);   // FakeVehicleRepository.findById letar här
        Database.getBookings().add(booking);
        Database.getMechanics().add(mechanic);
        Database.getServiceItems().add(service);
        Database.getWorkOrders().add(workOrder);

        try {
            // Act – försök starta ordern
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> workOrderService.startWorkOrder(9201));

            // Assert – rätt regelfel
            assertEquals("startWorkOrder.mechanicBusy", exception.getMessage());
            // Assert – inget ändrades i minnet
            assertEquals(WorkOrderStatus.CONFIRMED, workOrder.getStatus());
            // Assert – inget sparades (fejken fick aldrig något)
            assertNull(fakeMechanicRepository.saved);
        } finally {
            // Städa i omvänd ordning så att inget ligger kvar till andra tester
            Database.getWorkOrders().remove(workOrder);
            Database.getServiceItems().remove(service);
            Database.getMechanics().remove(mechanic);
            Database.getBookings().remove(booking);
            Database.getVehicles().remove(vehicle);
            Database.getCustomers().remove(customer);
        }
    }


}
