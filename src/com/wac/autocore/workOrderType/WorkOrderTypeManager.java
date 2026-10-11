package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.WorkOrder;

import java.util.ArrayList;

import static com.wac.autocore.workOrderType.WorkOrderTypeEnum.*;

public class WorkOrderTypeManager {
    //Manager spindeln i nätet
    //som att välja vilken typ av operation som
    //systemet ska utföra vid visst läge = vilken strategi ska vi ha?
    //Strategy kan vara i ett menyval, där de olika alternativen representerar olika operationer som ska utföras
    //Som i en switch sats, så körs en viss metod vid ett visst tillfälle

    private ArrayList<WorkOrder> createdWorkOrders = new ArrayList<>();
    private WorkOrderTypeEnum strategy;

    public void setStrategy(WorkOrderTypeEnum strategy) {
        this.strategy = strategy;
    }
    //När en ny arbetsorder typ tillkommer, lägg in en klass och lägg till i switchsatsen, metoden createWorkOrderType
    public WorkOrder createWorkOrderType(WorkOrderDto dto){
        if(this.strategy == null) {
            throw new IllegalStateException("Must put in strategy");
        }
        WorkOrderTypeInterface orderStrategy;
        switch (this.strategy) {
            case PLANNED: orderStrategy = new PlannedWorkOrder();
                break;

            case DROP_IN: orderStrategy = new DropInWorkOrder();
                break;

            case COMPLAINT: orderStrategy = new ComplaintWorkOrder();
                break;
                default:
                    throw new IllegalStateException("Unknown workOrderType");
        }
        WorkOrder newOrder = orderStrategy.createWorkOrder(dto);
        createdWorkOrders.add(newOrder);
        return newOrder;
    }

}
