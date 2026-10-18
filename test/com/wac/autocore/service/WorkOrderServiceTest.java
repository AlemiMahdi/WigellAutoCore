package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorkOrderServiceTest {
    private final   FakeWorkOrderRepository fakeRepository = new FakeWorkOrderRepository();
    private final WorkOrderService workOrderService = new WorkOrderService(fakeRepository, null, null);

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
    void createDraftWorkOrderWithoutTypeThrow(){
        int bookingBefore = Database.getBookings().size();

        assertThrows(IllegalArgumentException.class,
                () -> workOrderService.createNewDraftWorkOrder(1, "Bromsljus", null, null));

        assertEquals(bookingBefore, Database.getBookings().size());
        assertNull(fakeRepository.saveDraft);
    }
}
