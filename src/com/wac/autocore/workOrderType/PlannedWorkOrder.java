package com.wac.autocore.workOrderType;

import com.wac.autocore.model.WorkOrder;

public class PlannedWorkOrder implements WorkOrderTypeInterface{

    @Override
    public WorkOrder createWorkOrder(int bookingId, int mechanicId) {

        //Skapa variabel och sätt in type enum
        WorkOrder order = new WorkOrder();
        order.setBookingId(bookingId);
        order.setMechanicId(mechanicId);
        order.setType(WorkOrderTypeEnum.PLANNED);
        return order;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
