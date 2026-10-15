package com.wac.autocore.DTO;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;

public class WorkOrderResult {
    private final WorkOrder workOrder;
    private final Booking booking; // Kan vara null för Planned, eller innehålla skuggbokningen för Complaint/Drop-in

    public WorkOrderResult(WorkOrder workOrder, Booking booking) {
        this.workOrder = workOrder;
        this.booking = booking;
    }

    public WorkOrder getWorkOrder() { return workOrder; }
    public Booking getBooking() { return booking; }
}
