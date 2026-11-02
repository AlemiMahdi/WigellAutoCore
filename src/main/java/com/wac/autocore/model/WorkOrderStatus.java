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

    // Bara utkast får redigeras
    public boolean isEditable() {
        return this == DRAFT;
    }

    // bokningens tjänster får ändras när arbetsordern är CONFIRMED.
    public boolean allowsServiceChanges() {
        return this == CONFIRMED;
    }

}
