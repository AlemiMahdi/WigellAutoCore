package com.wac.autocore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void completedCannotChangeToInProgress() {
        assertFalse(WorkOrderStatus.COMPLETED.canChangeTo(WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void canceledCannotChangeToConfirmed() {
        assertFalse(WorkOrderStatus.CANCELLED.canChangeTo(WorkOrderStatus.CONFIRMED));
    }

    @Test
    void draftCannotChangeToDraft() {
        assertFalse(WorkOrderStatus.DRAFT.canChangeTo(WorkOrderStatus.DRAFT));
    }

    @Test
    void confirmedCannotChangeToCompleted() {
        assertFalse(WorkOrderStatus.CONFIRMED.canChangeTo(WorkOrderStatus.COMPLETED));
    }

    @Test
    void draftIsEditable() {
        assertTrue(WorkOrderStatus.DRAFT.isEditable());
    }

    @Test
    void confirmedIsNotEditable() {
        assertFalse(WorkOrderStatus.CONFIRMED.isEditable());
    }

    @Test
    void completedIsNotEditable() {
        assertFalse(WorkOrderStatus.COMPLETED.isEditable());
    }

    @Test
    void inProgressIsNotEditable() {
        assertFalse(WorkOrderStatus.IN_PROGRESS.isEditable());
    }

    @Test
    void canceledIsNotEditable() {
        assertFalse(WorkOrderStatus.CANCELLED.isEditable());
    }

    // allowsServiceChange(): bara CONFIRMED tillåter att bokningens tjänster ändras
    @Test
    void draftDoesNotAllowServiceChanges() {
        assertFalse(WorkOrderStatus.DRAFT.allowsServiceChanges());
    }

    @Test
    void confirmedAllowsServiceChanges() {
        assertTrue(WorkOrderStatus.CONFIRMED.allowsServiceChanges());
    }

    @Test
    void inProgressDoesNotAllowServiceChanges() {
        assertFalse(WorkOrderStatus.IN_PROGRESS.allowsServiceChanges());
    }

    @Test
    void completedDoesNotAllowServiceChanges() {
        assertFalse(WorkOrderStatus.COMPLETED.allowsServiceChanges());
    }

    @Test
    void canceledDoesNotAllowServiceChanges() {
        assertFalse(WorkOrderStatus.CANCELLED.allowsServiceChanges());
    }
}
