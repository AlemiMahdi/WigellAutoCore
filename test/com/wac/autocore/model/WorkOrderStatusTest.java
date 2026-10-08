package com.wac.autocore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static  org.junit.jupiter.api.Assertions.assertTrue;

public class WorkOrderStatusTest {

    @Test
    void draftCanChangeToConfirmed() {
        assertTrue(WorkOrderStatus.DRAFT.canChangeTo(WorkOrderStatus.CONFIRMED));
    }

    @Test
    void draftCanChangeToCancel() {
        assertTrue(WorkOrderStatus.DRAFT.canChangeTo(WorkOrderStatus.CANCELLED));
    }

    @Test
    void confirmedCanChangeToInProgress() {
        assertTrue(WorkOrderStatus.CONFIRMED.canChangeTo(WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void confirmedCanChangeToCanceled() {
        assertTrue(WorkOrderStatus.CONFIRMED.canChangeTo(WorkOrderStatus.CANCELLED));
    }

    @Test
    void inProgressCanChangeToCompleted() {
        assertTrue(WorkOrderStatus.IN_PROGRESS.canChangeTo(WorkOrderStatus.COMPLETED));
    }

    @Test
    void inProgressCanChangeToCanceled() {
        assertTrue(WorkOrderStatus.IN_PROGRESS.canChangeTo(WorkOrderStatus.CANCELLED));
    }

    @Test
    void draftCannotChangeToCompleted() {
        assertFalse(WorkOrderStatus.DRAFT.canChangeTo(WorkOrderStatus.COMPLETED));
    }

    @Test
    void confirmedCannotChangeToDraft() {
        assertFalse(WorkOrderStatus.CONFIRMED.canChangeTo(WorkOrderStatus.DRAFT));
    }

    @Test
    void inProgressCannotChangeToConfirmed() {
        assertFalse(WorkOrderStatus.IN_PROGRESS.canChangeTo(WorkOrderStatus.CONFIRMED));
    }

    @Test
    void completedCannotChangeToCanceled() {
        assertFalse(WorkOrderStatus.COMPLETED.canChangeTo(WorkOrderStatus.CANCELLED));
    }

    @Test
    void canceledCannotChangeToDraft() {
        assertFalse(WorkOrderStatus.CANCELLED.canChangeTo(WorkOrderStatus.DRAFT));
    }
}
