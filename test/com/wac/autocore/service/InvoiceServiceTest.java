package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceServiceTest {

    @Test
    void savesInvoiceThroughRepository() {
        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        InvoiceService service = new InvoiceService(repository);
        Invoice invoice = new Invoice(1, 1, LocalDate.now(), 500.0);

        service.save(invoice);

        assertSame(invoice, repository.savedInvoice);
        assertEquals(1, service.findAll().size());
        assertSame(invoice, service.findAll().get(0));
    }

    @Test
    void propagatesStorageFailure() {
        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        RuntimeException failure =
                new IllegalStateException("Storage unavailable");
        repository.saveFailure = failure;

        InvoiceService service = new InvoiceService(repository);
        Invoice invoice = new Invoice(1, 1, LocalDate.now(), 500.0);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.save(invoice)
        );

        assertSame(failure, thrown);
        assertTrue(repository.invoices.isEmpty());
    }

    private static class FakeInvoiceRepository
            implements IInvoiceRepository {

        private final List<Invoice> invoices = new ArrayList<>();
        private Invoice savedInvoice;
        private RuntimeException saveFailure;

        @Override
        public void save(Invoice invoice) {
            if (saveFailure != null) {
                throw saveFailure;
            }
            savedInvoice = invoice;
            invoices.add(invoice);
        }

        @Override
        public List<Invoice> findAll() {
            return new ArrayList<>(invoices);
        }

        @Override
        public List<Invoice> findAllInvoices() {
            return findAll();
        }
    }
}