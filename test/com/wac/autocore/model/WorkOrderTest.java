package com.wac.autocore.model;

import com.wac.autocore.workOrderType.WorkOrderTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testar att WorkOrder följer statusreglerna när statusen byts (WAC-62).
// Reglerna själva testas i WorkOrderStatusTest – här testas att WorkOrder använder dem.
public class WorkOrderTest {

    // Ett tillåtet byte (CONFIRMED → IN_PROGRESS) ska ändra statusen.
    @Test
    void allowedChangeUpdatesStatus() {
        // En ny arbetsorder börjar alltid som CONFIRMED
        WorkOrder workOrder = new WorkOrder(1, 1, 1, WorkOrderTypeEnum.PLANNED);

        workOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());
    }

    // Ett otillåtet byte (CONFIRMED → COMPLETED) ska stoppas med ett fel,
    // och statusen ska vara oförändrad efteråt.
    @Test
    void forbiddenChangeThrowsAndKeepsStatus() {
        WorkOrder workOrder = new WorkOrder(1, 1, 1, WorkOrderTypeEnum.PLANNED);

        // Hoppar över IN_PROGRESS – det är inte tillåtet
        assertThrows(IllegalStateException.class,
                () -> workOrder.changeStatus(WorkOrderStatus.COMPLETED));

        // Viktigast: felet ska komma INNAN statusen ändras
        assertEquals(WorkOrderStatus.CONFIRMED, workOrder.getStatus());
    }
}