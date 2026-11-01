package com.wac.autocore.standalone;


import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.IWorkOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Arbetsorderrepo för det fristående programmet (WAC-106).
 * Används inte av operationerna, men BookingService kräver det i konstruktorn.
 * Alla metoder kastar UnsupportedOperationException.
 * Ingen @Repository här, annars hittar Spring två repon i Main 1.
 */
public class InMemoryWorkOrderRepository implements IWorkOrderRepository {

    private static final String NOT_USED = "Not used in standalone program";

    @Override
    public WorkOrder save(WorkOrder workOrder) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public void update(WorkOrder workOrder) {
        throw new UnsupportedOperationException(NOT_USED);

    }

    @Override
    public void saveWithBooking(WorkOrder workOrder, Booking booking) {
        throw new UnsupportedOperationException(NOT_USED);

    }

    @Override
    public WorkOrder findWorkOrder(int id) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<WorkOrder> findAll() {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<String> confirmDraft(int workOrderId) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public void updateDraft(int draft, int bookingId, List<Integer> selectedIds, int mechanicId, BigDecimal normalizedPrice, String savedInstructions, String savedComments, Integer originalWorkOrderId, LocalDate plannedDate, int minutes) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public void saveDropInOrder(WorkOrder workOrder, Booking booking) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public void completeWorkOrder(int workOrderId) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<WorkOrder> findByBookingId(int bookingId) {
        throw new UnsupportedOperationException(NOT_USED);
    }
}
