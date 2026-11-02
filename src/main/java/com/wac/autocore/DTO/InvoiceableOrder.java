package com.wac.autocore.DTO;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.ui.views.ShowInvoiceView;

public class InvoiceableOrder {

    private final WorkOrder workOrder;
    private final String description;

    public InvoiceableOrder(WorkOrder workOrder, String brand, String model){
        this.workOrder = workOrder;
        this.description = ShowInvoiceView.workOrderCode(workOrder.getId()) + " . " + brand + " " + model;
    }

    public WorkOrder getWorkOrder(){
        return workOrder;
    }

    @Override
    public String toString() {
        return description;
    }
}
