package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;

import java.util.List;
import java.util.Objects;

public class InvoiceService {

    private final IInvoiceRepository invoiceRepository;

    public InvoiceService(IInvoiceRepository invoiceRepository) {
        this.invoiceRepository = Objects.requireNonNull(
                invoiceRepository, "Invoice repository is required"
        );
    }

    public void save(Invoice invoice) {
        Objects.requireNonNull(invoice, "Invoice is required");
        invoiceRepository.save(invoice);
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }
}