package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceServiceTest {

    private List<Invoice> originalInvoices;

    @BeforeEach
    void saveOriginalInvoices() {
        originalInvoices = new ArrayList<>(Database.getInvoices());
        Database.getInvoices().clear();
    }

    @AfterEach
    void restoreOriginalInvoices() {
        Database.getInvoices().clear();
        Database.getInvoices().addAll(originalInvoices);
    }

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

    @Test
    void createInvoiceSavesBeforeAddingToMemory() {
        Invoice invoice = new Invoice(1, 1, LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository() {
            @Override
            public void save(Invoice invoiceToSave) {
                assertFalse(Database.getInvoices().contains(invoiceToSave));
                super.save(invoiceToSave);
            }
        };

        InvoiceService service = new InvoiceService(
                repository, new StubGarageSystem(invoice)
        );

        Invoice result = service.createInvoice(1, "", "", 0.0);

        assertSame(invoice, result);
        assertSame(invoice, repository.savedInvoice);
        assertEquals(1, repository.invoices.size());
        assertEquals(1, Database.getInvoices().size());
        assertSame(invoice, Database.getInvoices().get(0));
    }

    @Test
    void createInvoiceStorageFailureLeavesMemoryUnchanged() {
        Invoice existing = new Invoice(1, 1, LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        Invoice invoice = new Invoice(2, 2, LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        RuntimeException failure =
                new IllegalStateException("Storage unavailable");
        repository.saveFailure = failure;

        InvoiceService service = new InvoiceService(
                repository, new StubGarageSystem(invoice)
        );

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.createInvoice(2, "", "", 0.0)
        );

        assertSame(failure, thrown);
        assertEquals(1, repository.saveCalls);
        assertTrue(repository.invoices.isEmpty());
        assertEquals(before, Database.getInvoices());
    }

    @Test
    void rejectedInvoiceDoesNotCallRepositoryOrChangeMemory() {
        Invoice existing = new Invoice(1, 1, LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        InvoiceService service = new InvoiceService(
                repository, new StubGarageSystem(null)
        );

        Invoice result = service.createInvoice(2, "", "", 0.0);

        assertNull(result);
        assertEquals(0, repository.saveCalls);
        assertEquals(before, Database.getInvoices());
    }

    private static class FakeInvoiceRepository
            implements IInvoiceRepository {

        private final List<Invoice> invoices = new ArrayList<>();
        private Invoice savedInvoice;
        private RuntimeException saveFailure;
        private int saveCalls;

        @Override
        public void save(Invoice invoice) {

            saveCalls++;

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

    private static class StubGarageSystem extends GarageSystem {

        private final Invoice preparedInvoice;

        private StubGarageSystem(Invoice preparedInvoice) {
            this.preparedInvoice = preparedInvoice;
        }

        @Override
        public Invoice prepareInvoice(
                int workOrderId,
                String discountCode,
                String extraDescription,
                double extraCost) {

            return preparedInvoice;
        }
    }

}