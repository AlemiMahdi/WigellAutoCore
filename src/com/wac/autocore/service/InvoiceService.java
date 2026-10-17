package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;

import java.util.List;
import java.util.Objects;

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
}