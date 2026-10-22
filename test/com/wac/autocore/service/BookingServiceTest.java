package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IBookingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookingServiceTest {

    private final IBookingRepository fakeBookingRepository = new IBookingRepository() {
        private int nextId = 1000;

        @Override
        public void save(Booking booking) {
            booking.setId(nextId++);
        }

        @Override
        public void save(Booking booking, int mechanicId) {
            booking.setId(nextId++);
        }

        @Override
        public List<Booking> findAll() {
            return new ArrayList<>();
        }

        @Override
        public Booking findById(int id) {
            return null;
        }
    };

    private final BookingService bookingService = new BookingService(fakeBookingRepository);

    // Bokningar som testet lägger i den delade listan och måste städa bort
    private final List<Booking> createdBookings = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        Database.getBookings().removeAll(createdBookings);
    }

    @Test
    void newBookingFromPreviousBookingIsSeparateAndUsesCurrentPrice() {
        ServiceItem oilChange = new ServiceItem("Oljebyte", "", 500, 30);
        ServiceItem tires = new ServiceItem("Däckbyte", "", 300, 30);

        int vehicleId = Database.getVehicles().get(0).getId();
        int mechanicId = Database.getMechanics().get(0).getId();

        // Förra årets bokning
        Booking previous = bookingService.createBooking(
                vehicleId,
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                60,
                "Årlig service",
                new ArrayList<>(Arrays.asList(oilChange, tires)),
                mechanicId);
        createdBookings.add(previous);

        // Priset på oljebyte höjs innan den nya bokningen görs
        oilChange.setPrice(600);

        // Ny bokning utifrån den gamla: samma tjänster, nytt datum
        Booking copy = bookingService.createBooking(
                previous.getVehicleId(),
                LocalDate.now().plusDays(30),
                LocalTime.of(10, 0),
                previous.getDurationMinutes(),
                previous.getDescription(),
                previous.getServices(),
                mechanicId);
        createdBookings.add(copy);

        // Separat bokning med eget ID och nytt datum
        assertNotSame(previous, copy);
        assertNotEquals(previous.getId(), copy.getId());
        assertNotEquals(previous.getDate(), copy.getDate());
        assertEquals("BOOKED", copy.getStatus());

        // Den nya bokningen får dagens pris, den gamla behåller sitt
        assertEquals(500, previous.getFrozenPrice().get(0).getPrice());
        assertEquals(600, copy.getFrozenPrice().get(0).getPrice());

        // Ändringar i den nya bokningen påverkar inte den gamla
        copy.getServices().remove(tires);
        assertEquals(2, previous.getServices().size());
        assertTrue(previous.getServices().contains(tires));
    }
}