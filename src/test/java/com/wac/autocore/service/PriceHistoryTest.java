package com.wac.autocore.service;

import com.wac.autocore.DTO.InvoiceableOrder;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.repository.IBookingRepository;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.repository.IServiceItemRepository;
import com.wac.autocore.repository.IWorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Regressionstest för prishistorik
// Körs utan databas och Spring: repositories ersätts med enkla fakes.
public class PriceHistoryTest {

    private final IWorkOrderRepository fakeWorkOrderRepository = new FakeWorkOrderRepository();
    private final IServiceItemRepository fakeServiceItemRepository = new FakeServiceItemRepository();
    // Fake: ger varje sparad bokning ett nytt ID, som databasen gör
    private final IBookingRepository fakeBookingRepository = new IBookingRepository() {
        private int nextId = 1000;

        @Override
        public Booking save(Booking booking) {
            booking.setId(nextId++);
            return booking;
        }

        @Override
        public Booking save(Booking booking, int mechanicId) {
            return save(booking);
        }

        @Override
        public List<Booking> findAll() {
            return new ArrayList<>();
        }

        @Override
        public Booking findById(int id) {
            return null;
        }

        @Override
        public int updateBookingServices(int bookingId, List<Integer> serviceIds) {
            throw new UnsupportedOperationException(
                    "updateBookingServices is not implemented in this test fake"
            );
        }

        @Override
        public List<Booking> findOtherBookingsByMechanic(int mechanicId, int bookingId) {
            return List.of();
        }

        @Override
        public Booking findByIdAndServices(int id) {
            return null;
        }

        @Override
        public void update(Booking booking) {

        }

        @Override
        public void updateBookingAndOrder(List<WorkOrder> orderList, Booking booking) {

        }
    };

    // Fake: inga tidigare fakturor
    private final IInvoiceRepository fakeInvoiceRepository = new IInvoiceRepository() {
        @Override
        public Invoice save(Invoice invoice) {
            return invoice;
        }

        @Override
        public List<Invoice> findAll() {
            return new ArrayList<>();
        }

        @Override
        public List<InvoiceableOrder> findInvoiceableWorkOrdersWithVehicle() {
            return List.of();
        }
    };

    private final BookingService bookingService = new BookingService(fakeBookingRepository, fakeWorkOrderRepository, fakeServiceItemRepository);
    private final ServiceItemService serviceItemService = new ServiceItemService(fakeServiceItemRepository);

    // Bokning som testet lägger i den delade listan och måste städa bort
    private Booking createdBooking;

    @AfterEach
    void cleanUp() {
        Database.getBookings().remove(createdBooking);
    }

    @Test
    void priceChangeAfterBookingDoesNotChangeBookingOrInvoice() {
        ServiceItem oilChange = new ServiceItem("Oljebyte", "", 500, 30);
        ServiceItem tires = new ServiceItem("Däckbyte", "", 300, 30);

        int vehicleId = Database.getVehicles().get(0).getId();
        int mechanicId = Database.getMechanics().get(0).getId();

        // Kunden bokar när oljebyte kostar 500 kr
        createdBooking = bookingService.createBooking(
                vehicleId,
                LocalDate.now().plusDays(3),
                LocalTime.of(13, 0),
                60,
                "Service",
                new ArrayList<>(Arrays.asList(oilChange, tires)),
                mechanicId);

        // Verkstaden höjer priset innan arbetet utförs
        serviceItemService.savePrice(oilChange, 800);
        assertEquals(800, oilChange.getPrice());

        // Bokningen behåller priset från bokningstillfället
        assertEquals(500, createdBooking.getFrozenPrice().get(0).getPrice());

        // Arbetet utförs och faktureras
        WorkOrder order = new WorkOrder(createdBooking.getId(), mechanicId, WorkOrderTypeEnum.PLANNED);
        order.setId(1);
        order.changeStatus(WorkOrderStatus.IN_PROGRESS);
        order.changeStatus(WorkOrderStatus.COMPLETED);

        InvoiceService invoiceService = new InvoiceService(
                fakeInvoiceRepository,
                () -> List.of(order),
                () -> List.of(createdBooking),
                ArrayList::new,
                ArrayList::new,
                () -> List.of(oilChange, tires));

        Invoice invoice = invoiceService.previewInvoice(order.getId(), "", "", 0.0);

        // Fakturan använder 500 + 300, inte dagens pris 800 + 300
        assertEquals(800.0, invoice.getAmount(), 0.001);
        assertEquals(500.0, invoice.getLines().get(0).getPrice(), 0.001);

        assertEquals(2, invoice.getLines().size());
        assertEquals(300.0, invoice.getLines().get(1).getPrice(), 0.001);
    }
}