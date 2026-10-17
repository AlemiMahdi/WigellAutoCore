package com.wac.autocore.service;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;

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

    @Test
    void previewInvoiceDoesNotSaveOrChangeMemory() {
        Invoice existing = new Invoice(1, 1, LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        Invoice prepared = new Invoice(2, 2, LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository();

        InvoiceService service = new InvoiceService(
                repository,
                new StubGarageSystem(prepared)
        );

        Invoice preview = service.previewInvoice(2, "", "", 0.0);

        assertSame(prepared, preview);
        assertEquals(0, repository.saveCalls);
        assertTrue(repository.invoices.isEmpty());
        assertEquals(before, Database.getInvoices());
    }

    @Test
    void previewAndCreationUseSamePricesAndDiscount() {
        List<Booking> originalBookings =
                new ArrayList<>(Database.getBookings());
        List<WorkOrder> originalOrders =
                new ArrayList<>(Database.getWorkOrders());
        List<Vehicle> originalVehicles =
                new ArrayList<>(Database.getVehicles());
        List<Customer> originalCustomers =
                new ArrayList<>(Database.getCustomers());

        try {
            Database.getBookings().clear();
            Database.getWorkOrders().clear();
            Database.getVehicles().clear();
            Database.getCustomers().clear();

            Customer customer = new Customer(
                    1, "Testkund", "0701234567", "test@example.com"
            );
            Vehicle vehicle = new Vehicle(
                    1, "ABC123", "Volvo", "V70", 2015, customer.getId()
            );

            Booking booking = new Booking(
                    1, vehicle.getId(), LocalDate.of(2026, 10, 17),
                    "Testbokning"
            );
            booking.addFrozenPrice(
                    new InvoiceLine(1, "Oil change", 1000.0)
            );
            booking.addFrozenPrice(
                    new InvoiceLine(2, "Brake service", 500.0)
            );

            WorkOrder order = new WorkOrder(
                    1, booking.getId(), 1, WorkOrderTypeEnum.PLANNED
            );
            order.addServiceItem(1);
            order.addServiceItem(2);
            order.changeStatus(WorkOrderStatus.IN_PROGRESS);
            order.changeStatus(WorkOrderStatus.COMPLETED);

            Database.getCustomers().add(customer);
            Database.getVehicles().add(vehicle);
            Database.getBookings().add(booking);
            Database.getWorkOrders().add(order);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();
            InvoiceService service = new InvoiceService(
                    repository, new GarageSystem()
            );

            Invoice preview = service.previewInvoice(
                    order.getId(), "WELCOME10", "", 0.0
            );

            assertNotNull(preview);
            assertEquals(1500.0, preview.getAmount(), 0.001);
            assertEquals(150.0, preview.getDiscount(), 0.001);
            assertEquals(1350.0, preview.getTotalAmount(), 0.001);
            assertEquals(2, preview.getLines().size());

            // Förhandsvisningen får inte spara eller registrera fakturan.
            assertEquals(0, repository.saveCalls);
            assertTrue(Database.getInvoices().isEmpty());

            Invoice created = service.createInvoice(
                    order.getId(), "WELCOME10", "", 0.0
            );

            assertNotNull(created);
            assertNotSame(preview, created);
            assertEquals(preview.getAmount(), created.getAmount(), 0.001);
            assertEquals(preview.getDiscount(), created.getDiscount(), 0.001);
            assertEquals(
                    preview.getTotalAmount(),
                    created.getTotalAmount(),
                    0.001
            );
            assertEquals(2, created.getLines().size());

            double lineTotal = created.getLines().stream()
                    .mapToDouble(InvoiceLine::getFinalPrice)
                    .sum();

            assertEquals(created.getTotalAmount(), lineTotal, 0.001);
            assertEquals(1, repository.saveCalls);
            assertSame(created, repository.savedInvoice);
            assertEquals(1, Database.getInvoices().size());
            assertSame(created, Database.getInvoices().get(0));

            // Originalets sparade priser ska vara oförändrade.
            assertEquals(
                    1000.0, booking.getFrozenPrice().get(0).getPrice(), 0.001
            );
            assertEquals(
                    500.0, booking.getFrozenPrice().get(1).getPrice(), 0.001
            );

        } finally {
            Database.getBookings().clear();
            Database.getBookings().addAll(originalBookings);

            Database.getWorkOrders().clear();
            Database.getWorkOrders().addAll(originalOrders);

            Database.getVehicles().clear();
            Database.getVehicles().addAll(originalVehicles);

            Database.getCustomers().clear();
            Database.getCustomers().addAll(originalCustomers);
        }
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