package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.Label;

public interface WorkOrderTypeInterface {
    //vad ska finnas med i WorkOrderType
    //metod som ska finnas med för alla
    //alla kan implementera sina olika lösningar i metoden sen
    //Planerad arbetsorder, drop-in, reklamation med ENUM
    //Manager spindeln i nätet

    WorkOrder createWorkOrder(WorkOrderDto workOrderDto, LanguageManager language, Label feedbackLabel);
    void startWorkOrder(int workOrderId);
    void completeWorkOrder(int workOrderId);
}
