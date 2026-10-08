package com.wac.autocore.workOrderType;

import com.wac.autocore.model.WorkOrder;

public class DropInWorkOrder implements WorkOrderTypeInterface{
    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {
        return null;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
