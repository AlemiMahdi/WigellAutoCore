package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.WorkOrder;

public class ComplaintWorkOrder implements WorkOrderTypeInterface{


    @Override
    public WorkOrder createWorkOrder(WorkOrderDto workOrderDto) {
        return null;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
