package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.ui.language.LanguageManager;

import java.util.ArrayList;

public class WorkOrderTypeManager {
    //Manager spindeln i nätet
    //som att välja vilken typ av operation som
    //systemet ska utföra vid visst läge = vilken strategi ska vi ha?
    //Strategy kan vara i ett menyval, där de olika alternativen representerar olika operationer som ska utföras
    //Som i en switch sats, så körs en viss metod vid ett visst tillfälle

    public WorkOrder createWorkOrderType(WorkOrderTypeEnum type, WorkOrderDto dto, LanguageManager language) {
        WorkOrderTypeInterface strategy;
        switch (type) {
            case PLANNED:
                strategy = new PlannedWorkOrder();
                break;
            case COMPLAINT:
                strategy = new ComplaintWorkOrder();
                break;
            case DROP_IN:
                strategy = new DropInWorkOrder();
                break;
            default:
                throw new IllegalArgumentException("createWorkOrder.unknownType");
        }
        return strategy.createWorkOrder(dto, language);
    }
}
