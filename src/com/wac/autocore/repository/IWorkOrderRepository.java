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
    List<String> confirmDraft(int workOrderId);
    void updateDraft(
            int draft,
            int bookingId,
            List<Integer> selectedIds,
            int mechanicId,
            BigDecimal normalizedPrice,
            String savedInstructions,
            String savedComments,
            Integer originalWorkOrderId,
            LocalDate plannedDate,
            int minutes
    );
    void saveDropInOrder(WorkOrder workOrder, Booking booking);
    List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems);
    void completeWorkOrder(int workOrderId);

}
