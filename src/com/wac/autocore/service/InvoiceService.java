package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.service.exception.InvoiceValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;

import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class InvoiceService {

    private final IInvoiceRepository invoiceRepository;
    private final GarageSystem garageSystem;

    public InvoiceService(IInvoiceRepository invoiceRepository) {

        this(invoiceRepository, new GarageSystem());
    }

    public InvoiceService(
            IInvoiceRepository invoiceRepository,
            GarageSystem garageSystem) {

        this.invoiceRepository = Objects.requireNonNull(
                invoiceRepository, "Invoice repository is required"
        );
        this.garageSystem = Objects.requireNonNull(
                garageSystem, "Garage system is required"
        );
    }

    public Invoice createInvoice(



            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        validateExtraCost(extraDescription, extraCost);

        Invoice invoice = garageSystem.prepareInvoice(
                workOrderId,
                discountCode,
                extraDescription,
                extraCost
        );

        if (invoice == null) {
            return null;
        }

        invoiceRepository.save(invoice);
        Database.getInvoices().add(invoice);

        return invoice;
    }

    public Invoice previewInvoice(
            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        validateExtraCost(extraDescription, extraCost);

        return garageSystem.prepareInvoice(
                workOrderId,
                discountCode,
                extraDescription,
                extraCost
        );
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public void save(Invoice invoice) {
        Objects.requireNonNull(invoice, "Invoice is required");
        invoiceRepository.save(invoice);
    }

    private void validateExtraCost(
            String extraDescription,
            double extraCost) {

        if (!Double.isFinite(extraCost) || extraCost < 0) {
            throw new InvoiceValidationException(
                    "createInvoice.invalidExtraCost"
            );
        }

        try {
            BigDecimal.valueOf(extraCost)
                    .setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvoiceValidationException(
                    "createInvoice.invalidExtraCost", exception
            );
        }

        if (extraCost > 0
                && (extraDescription == null
                || extraDescription.trim().isEmpty())) {
            throw new InvoiceValidationException(
                    "createInvoice.missingExtraDescription"
            );
        }
    }

    public List<Invoice> getLoadedInvoices() {
        return new ArrayList<>(Database.getInvoices());
    }

    public List<WorkOrder> getInvoiceableWorkOrders() {
        return Database.getWorkOrders().stream()
                .filter(order -> order.getStatus() == WorkOrderStatus.COMPLETED)
                .filter(order -> Database.getInvoices().stream()
                        .noneMatch(invoice ->
                                invoice.getWorkOrderId() == order.getId()))
                .collect(Collectors.toList());
    }

    public Vehicle findVehicleForWorkOrder(int workOrderId) {
        WorkOrder order = Database.getWorkOrders().stream()
                .filter(item -> item.getId() == workOrderId)
                .findFirst()
                .orElse(null);

        if (order == null) {
            return null;
        }

        Booking booking = Database.getBookings().stream()
                .filter(item -> item.getId() == order.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            return null;
        }

        return Database.getVehicles().stream()
                .filter(item -> item.getId() == booking.getVehicleId())
                .findFirst()
                .orElse(null);
    }

}