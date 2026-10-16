package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IWorkOrderRepository {

    void save(WorkOrder workOrder);
    void saveWithBooking(WorkOrder workOrder, Booking booking);
    WorkOrder findWorkOrder(int id);
    List<WorkOrder> findAll();
    List<WorkOrder> findAllWorkOrders();
    List<String> confirmDraft(int workOrderId);
    void updateDraft(
            WorkOrder draft,
            Booking booking,
            List<Integer> serviceIds,
            int mechanicId,
            LocalDate plannedDate,
            int minutes,
            BigDecimal price,
            String instructions,
            String comments,
            Integer originalWorkOrderId
    );
    void saveDropInOrder(WorkOrder workOrder, Booking booking);
    List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems);

}
