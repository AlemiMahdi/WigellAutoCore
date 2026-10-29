package com.wac.autocore.service;

import com.wac.autocore.DTO.InvoiceableOrder;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.exception.InvoiceValidationException;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.model.ServiceItem;

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
        InvoiceService service = createService(repository);
        Invoice invoice = new Invoice(1, LocalDate.now(), 500.0);
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

        InvoiceService service = createService(repository);
        Invoice invoice = new Invoice(1, LocalDate.now(), 500.0);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.save(invoice)
        );

        assertSame(failure, thrown);
        assertTrue(repository.invoices.isEmpty());
    }

    @Test
    void createInvoiceSavesBeforeAddingToMemory() {
        Invoice invoice = new Invoice(1, LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository() {
            @Override
            public Invoice save(Invoice invoiceToSave) {
                assertFalse(Database.getInvoices().contains(invoiceToSave));
                return super.save(invoiceToSave);
            }
        };

        InvoiceService service = new StubInvoiceService(
                repository, invoice
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
        Invoice existing = new Invoice(1,LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        Invoice invoice = new Invoice(2,LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        RuntimeException failure =
                new IllegalStateException("Storage unavailable");
        repository.saveFailure = failure;

        InvoiceService service = new StubInvoiceService(
                repository, invoice
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
        Invoice existing = new Invoice(1, LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        InvoiceService service = new StubInvoiceService(
                repository, null
        );

        Invoice result = service.createInvoice(2, "", "", 0.0);

        assertNull(result);
        assertEquals(0, repository.saveCalls);
        assertEquals(before, Database.getInvoices());
    }

    @Test
    void previewInvoiceDoesNotSaveOrChangeMemory() {
        Invoice existing = new Invoice(1, LocalDate.now(), 200.0);
        Database.getInvoices().add(existing);
        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        Invoice prepared = new Invoice(2, LocalDate.now(), 500.0);
        FakeInvoiceRepository repository = new FakeInvoiceRepository();

        InvoiceService service = new StubInvoiceService(
                repository, prepared
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

            WorkOrder order = new WorkOrder(booking.getId(), 1, WorkOrderTypeEnum.PLANNED
            );
            order.setId(1);

            order.addServiceItem(1);
            order.addServiceItem(2);
            order.changeStatus(WorkOrderStatus.IN_PROGRESS);
            order.changeStatus(WorkOrderStatus.COMPLETED);

            Database.getCustomers().add(customer);
            Database.getVehicles().add(vehicle);
            Database.getBookings().add(booking);
            Database.getWorkOrders().add(order);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();
            InvoiceService service = createService(repository);

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

    @Test
    void rejectsExtraCostWithMoreThanTwoDecimals() {
        assertInvalidExtraCost(
                100.123,
                "Extra arbete",
                "createInvoice.invalidExtraCost"
        );
    }

    @Test
    void rejectsNegativeExtraCost() {
        assertInvalidExtraCost(
                -100.0,
                "Extra arbete",
                "createInvoice.invalidExtraCost"
        );
    }

    @Test
    void rejectsExtraCostWithoutDescription() {
        assertInvalidExtraCost(
                100.0,
                "   ",
                "createInvoice.missingExtraDescription"
        );
    }

    @Test
    void onlyCompletedOrdersWithoutInvoiceAreListed() {
        List<WorkOrder> originalOrders =
                new ArrayList<>(Database.getWorkOrders());

        try {
            Database.getWorkOrders().clear();

            WorkOrder completed = new WorkOrder(
                    1, 1, WorkOrderTypeEnum.PLANNED
            );
            completed.changeStatus(WorkOrderStatus.IN_PROGRESS);
            completed.changeStatus(WorkOrderStatus.COMPLETED);

            WorkOrder alreadyInvoiced = new WorkOrder(2, 1, WorkOrderTypeEnum.PLANNED
            );
            alreadyInvoiced.changeStatus(WorkOrderStatus.IN_PROGRESS);
            alreadyInvoiced.changeStatus(WorkOrderStatus.COMPLETED);

            WorkOrder confirmed = new WorkOrder(3, 1, WorkOrderTypeEnum.PLANNED
            );


            WorkOrder draft = WorkOrder.createDraft(
                    4, 4, WorkOrderTypeEnum.PLANNED
            );
            completed.setId(1);
            alreadyInvoiced.setId(2);
            confirmed.setId(3);

            Database.getWorkOrders().add(completed);
            Database.getWorkOrders().add(alreadyInvoiced);
            Database.getWorkOrders().add(confirmed);
            Database.getWorkOrders().add(draft);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();

            repository.save(
                    new Invoice(
                            alreadyInvoiced.getId(),
                            LocalDate.now(),
                            500.0
                    )
            );

            InvoiceService service = createService(repository);

            List<InvoiceableOrder> result = service.getInvoiceableWorkOrders();

            assertEquals(1, result.size());
            assertSame(completed, result.get(0).getWorkOrder());

            // Att ändra resultatlistan ska inte ändra originalets lista.
            result.clear();
            assertEquals(4, Database.getWorkOrders().size());

        } finally {
            Database.getWorkOrders().clear();
            Database.getWorkOrders().addAll(originalOrders);
        }
    }

    @Test
    void missingBookingStopsPreviewAndCreation() {
        List<WorkOrder> originalOrders =
                new ArrayList<>(Database.getWorkOrders());
        List<Booking> originalBookings =
                new ArrayList<>(Database.getBookings());

        try {
            Database.getWorkOrders().clear();
            Database.getBookings().clear();

            WorkOrder order = new WorkOrder(1, 1, WorkOrderTypeEnum.PLANNED
            );
            order.setId(1);

            order.changeStatus(WorkOrderStatus.IN_PROGRESS);
            order.changeStatus(WorkOrderStatus.COMPLETED);
            Database.getWorkOrders().add(order);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();
            InvoiceService service = createService(repository);

            InvoiceValidationException previewError = assertThrows(
                    InvoiceValidationException.class,
                    () -> service.previewInvoice(1, "", "", 0.0)
            );

            InvoiceValidationException creationError = assertThrows(
                    InvoiceValidationException.class,
                    () -> service.createInvoice(1, "", "", 0.0)
            );

            assertEquals(
                    "createInvoice.bookingMissing",
                    previewError.getMessage()
            );
            assertEquals(
                    "createInvoice.bookingMissing",
                    creationError.getMessage()
            );
            assertEquals(0, repository.saveCalls);
            assertTrue(Database.getInvoices().isEmpty());

        } finally {
            Database.getWorkOrders().clear();
            Database.getWorkOrders().addAll(originalOrders);
            Database.getBookings().clear();
            Database.getBookings().addAll(originalBookings);
        }
    }

    @Test
    void missingPriceHistoryStopsPreviewAndCreation() {
        List<WorkOrder> originalOrders =
                new ArrayList<>(Database.getWorkOrders());
        List<Booking> originalBookings =
                new ArrayList<>(Database.getBookings());

        try {
            Database.getWorkOrders().clear();
            Database.getBookings().clear();

            Booking booking = new Booking(
                    1, 1, LocalDate.now(), "Bokning utan sparade priser"
            );
            Database.getBookings().add(booking);

            WorkOrder order = new WorkOrder(booking.getId(), 1, WorkOrderTypeEnum.PLANNED
            );
            order.setId(1);

            order.changeStatus(WorkOrderStatus.IN_PROGRESS);
            order.changeStatus(WorkOrderStatus.COMPLETED);
            Database.getWorkOrders().add(order);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();
            InvoiceService service = createService(repository);

            InvoiceValidationException previewError = assertThrows(
                    InvoiceValidationException.class,
                    () -> service.previewInvoice(1, "", "", 0.0)
            );

            InvoiceValidationException creationError = assertThrows(
                    InvoiceValidationException.class,
                    () -> service.createInvoice(1, "", "", 0.0)
            );

            assertEquals(
                    "createInvoice.missingPriceHistory",
                    previewError.getMessage()
            );
            assertEquals(
                    "createInvoice.missingPriceHistory",
                    creationError.getMessage()
            );
            assertEquals(0, repository.saveCalls);
            assertTrue(Database.getInvoices().isEmpty());

        } finally {
            Database.getWorkOrders().clear();
            Database.getWorkOrders().addAll(originalOrders);
            Database.getBookings().clear();
            Database.getBookings().addAll(originalBookings);
        }
    }

    @Test
    void complaintWithoutPriceHistoryCanBeInvoicedAtZero() {
        List<WorkOrder> originalOrders =
                new ArrayList<>(Database.getWorkOrders());
        List<Booking> originalBookings =
                new ArrayList<>(Database.getBookings());
        List<ServiceItem> originalServices =
                new ArrayList<>(Database.getServiceItems());

        try {
            Database.getWorkOrders().clear();
            Database.getBookings().clear();
            Database.getServiceItems().clear();

            ServiceItem serviceItem = new ServiceItem(
                    "Oil change", "Testtjänst", 1000.0, 60
            );
            // Fast ID enbart för testdata, utan databasanslutning.
            serviceItem.setId(1);
            Database.getServiceItems().add(serviceItem);

            Booking booking = new Booking(
                    1, 1, LocalDate.now(), "Reklamation"
            );
            Database.getBookings().add(booking);

            WorkOrder order = new WorkOrder(booking.getId(), 1, WorkOrderTypeEnum.COMPLAINT
            );
            order.setId(1);
            order.addServiceItem(serviceItem.getId());
            order.changeStatus(WorkOrderStatus.IN_PROGRESS);
            order.changeStatus(WorkOrderStatus.COMPLETED);
            Database.getWorkOrders().add(order);

            FakeInvoiceRepository repository = new FakeInvoiceRepository();
            InvoiceService service = createService(repository);

            Invoice preview = service.previewInvoice(1, "", "", 0.0);

            assertNotNull(preview);
            assertEquals(1, preview.getLines().size());
            assertEquals(
                    "Oil change",
                    preview.getLines().get(0).getServiceName()
            );
            assertEquals(0.0, preview.getTotalAmount(), 0.001);
            assertEquals(0, repository.saveCalls);
            assertTrue(Database.getInvoices().isEmpty());

            Invoice created = service.createInvoice(1, "", "", 0.0);

            assertNotNull(created);
            assertEquals(1, created.getLines().size());
            assertEquals(0.0, created.getTotalAmount(), 0.001);
            assertEquals(1, repository.saveCalls);
            assertSame(created, repository.savedInvoice);
            assertEquals(1, Database.getInvoices().size());
            assertSame(created, Database.getInvoices().get(0));

        } finally {
            Database.getWorkOrders().clear();
            Database.getWorkOrders().addAll(originalOrders);
            Database.getBookings().clear();
            Database.getBookings().addAll(originalBookings);
            Database.getServiceItems().clear();
            Database.getServiceItems().addAll(originalServices);
        }
    }

    @Test
    void vipDiscountAndDiscountCodeAreCombined() {
        Customer vipCustomer = new Customer(
                1, "VIP-kund", "0701234567", "vip@example.com"
        );
        vipCustomer.setVip(true);

        Vehicle vehicle = new Vehicle(
                1, "VIP123", "Volvo", "XC60", 2020, vipCustomer.getId()
        );

        Booking booking = new Booking(
                1, vehicle.getId(), LocalDate.of(2026, 10, 23), "Service"
        );
        booking.addFrozenPrice(new InvoiceLine(1, "Oljebyte", 1000.0));
        booking.addFrozenPrice(new InvoiceLine(2, "Bromsservice", 500.0));

        WorkOrder order = new WorkOrder(
                booking.getId(), 1, WorkOrderTypeEnum.PLANNED
        );
        order.setId(1);
        order.changeStatus(WorkOrderStatus.IN_PROGRESS);
        order.changeStatus(WorkOrderStatus.COMPLETED);

        // Egna listor i stället för Database, så att inget delat ändras
        InvoiceService service = new InvoiceService(
                new FakeInvoiceRepository(),
                () -> List.of(order),
                () -> List.of(booking),
                () -> List.of(vehicle),
                () -> List.of(vipCustomer),
                ArrayList::new
        );

        Invoice invoice = service.previewInvoice(
                order.getId(), "WELCOME10", "", 0.0
        );

        // 1500 kr, 10 % VIP + 10 % WELCOME10 = 300 kr rabatt
        assertEquals(1500.0, invoice.getAmount(), 0.001);
        assertEquals(300.0, invoice.getDiscount(), 0.001);
        assertEquals(1200.0, invoice.getTotalAmount(), 0.001);

        // Rabatten är fördelad på raderna, så att raderna summerar till totalen
        double lineTotal = invoice.getLines().stream()
                .mapToDouble(InvoiceLine::getFinalPrice)
                .sum();
        assertEquals(invoice.getTotalAmount(), lineTotal, 0.001);
    }

    private InvoiceService createService(IInvoiceRepository repository) {
        return new InvoiceService(
                repository,
                Database::getWorkOrders,
                Database::getBookings,
                Database::getVehicles,
                Database::getCustomers,
                Database::getServiceItems
        );
    }

    private static class FakeInvoiceRepository
            implements IInvoiceRepository {

        private final List<Invoice> invoices = new ArrayList<>();
        private Invoice savedInvoice;
        private RuntimeException saveFailure;
        private int saveCalls;

        @Override
        public Invoice save(Invoice invoice) {
            saveCalls++;

            if (saveFailure != null) {
                throw saveFailure;
            }

            savedInvoice = invoice;
            invoices.add(invoice);

            return invoice;
        }

        @Override
        public List<Invoice> findAll() {
            return new ArrayList<>(invoices);
        }

        @Override
        public List<InvoiceableOrder> findInvoiceableWorkOrdersWithVehicle() {
            List<InvoiceableOrder> fakeList = new ArrayList<>();
            List<Invoice> invoices = this.findAll();

            for(WorkOrder order : Database.getWorkOrders()) {
                if(order.getStatus() == WorkOrderStatus.COMPLETED) {
                    boolean alreadyInvoiced = invoices.stream()
                            .anyMatch(invoice -> invoice.getWorkOrderId() == order.getId());
                    if (!alreadyInvoiced) {
                        fakeList.add(new InvoiceableOrder(order, "Volvo", "V90"));
                    }
                }
            } return fakeList;
        }
    }

    private static class StubInvoiceService extends InvoiceService {

        private final Invoice preparedInvoice;

        private StubInvoiceService(
                IInvoiceRepository repository,
                Invoice preparedInvoice) {

            super(
                    repository,
                    Database::getWorkOrders,
                    Database::getBookings,
                    Database::getVehicles,
                    Database::getCustomers,
                    Database::getServiceItems
            );
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

    private void assertInvalidExtraCost(
            double extraCost,
            String description,
            String expectedMessage) {

        FakeInvoiceRepository repository = new FakeInvoiceRepository();
        Invoice prepared = new Invoice(1, LocalDate.now(), 500.0);
        InvoiceService service = new StubInvoiceService(
                repository, prepared
        );

        List<Invoice> before = new ArrayList<>(Database.getInvoices());

        InvoiceValidationException previewError = assertThrows(
                InvoiceValidationException.class,
                () -> service.previewInvoice(1, "", description, extraCost)
        );

        InvoiceValidationException creationError = assertThrows(
                InvoiceValidationException.class,
                () -> service.createInvoice(1, "", description, extraCost)
        );

        assertEquals(expectedMessage, previewError.getMessage());
        assertEquals(expectedMessage, creationError.getMessage());
        assertEquals(0, repository.saveCalls);
        assertEquals(before, Database.getInvoices());
    }

}