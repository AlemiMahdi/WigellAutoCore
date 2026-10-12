package com.wac.autocore.model;

public enum WorkOrderStatus {
    DRAFT,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean canChangeTo(WorkOrderStatus next) {
        switch (this) {
            case DRAFT:
                return next == CONFIRMED || next == CANCELLED;
            case CONFIRMED:
                return next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS:
                return next == COMPLETED || next == CANCELLED;
            default:
                return false;
        }
    }

}
