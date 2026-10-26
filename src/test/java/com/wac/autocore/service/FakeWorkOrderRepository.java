package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.IWorkOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class FakeWorkOrderRepository implements IWorkOrderRepository {

    public WorkOrder saveDraft;
    public Booking saveBooking;

    @Override
    public WorkOrder save(WorkOrder workOrder) {
        return workOrder;
    }

    @Override
    public void saveWithBooking(WorkOrder workOrder, Booking booking) {
        saveDraft = workOrder;
        saveBooking = booking;

    }

    @Override
    public WorkOrder findWorkOrder(int id) {
        return null;
    }

    @Override
    public List<WorkOrder> findAll() {
        return Collections.emptyList();
    }

    @Override
    public List<String> confirmDraft(int workOrderId) {
        return Collections.emptyList();
    }

    @Override
    public void updateDraft(int draft, int bookingId, List<Integer> selectedIds, int mechanicId, BigDecimal normalizedPrice, String savedInstructions, String savedComments, Integer originalWorkOrderId, LocalDate plannedDate, int minutes) {

    }

    @Override
    public void saveDropInOrder(WorkOrder workOrder, Booking booking) {

    }

    @Override
    public List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems) {
        return Collections.emptyList();
    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }

    @Override
    public List<WorkOrder> findByBookingId(int bookingId) {
        return List.of();
    }
}
