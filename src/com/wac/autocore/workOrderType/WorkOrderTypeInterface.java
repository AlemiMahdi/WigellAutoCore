package com.wac.autocore.workOrderType;

import com.wac.autocore.model.WorkOrder;

public interface WorkOrderTypeInterface {
    //vad ska finnas med i WorkOrderType
    //metod som ska finnas med för alla
    //alla kan implementera sina olika lösningar i metoden sen
    //Planerad arbetsorder, drop-in, reklamation med ENUM
    //Manager spindeln i nätet

    WorkOrder createWorkOrder(int bookingId,
                              int mechanicId);
    void startWorkOrder(int workOrderId);
    void completeWorkOrder(int workOrderId);
}
