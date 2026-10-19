package com.wac.autocore.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IBookingRepository;

public class BookingService {

    private final IBookingRepository bookingRepository;

    public BookingService(IBookingRepository bookingRepository){
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> getAllBookings(){
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(int id ) {
        return Optional.ofNullable(bookingRepository.findById(id));
    }

    public Booking createBooking(
        int vehicleId,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes,
        String description,
        List<ServiceItem> services,
        int mechanicId
    ) {

        // 1. Kontrollera att fordonet finns
        boolean vehicleExists = Database.getVehicles().stream()
                .anyMatch(vehicle -> vehicle.getId() == vehicleId);

        if (!vehicleExists) {
            throw new IllegalArgumentException("createBooking.selectVehicle");
        }

        // 2. Validera bokningsuppgifter
        if (date == null) {
            throw new IllegalArgumentException("createBooking.selectDate");
        }

        if (startTime == null) {
            throw new IllegalArgumentException("createBooking.invalidStartTime");
        }

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("createBooking.durationGreaterThanZero");
        }

        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("createBooking.selectService");
        }

        boolean mechanicExists = Database.getMechanics().stream()
                .anyMatch(mechanic -> mechanic.getId() == mechanicId);

        if (!mechanicExists) {
            throw new IllegalArgumentException("createBooking.selectMechanic");
        }

        // 3. Kontrollera överlappande bokningar
        if (hasOverlappingBooking(
                mechanicId, date, startTime, durationMinutes)) {

            throw new IllegalArgumentException("createBooking.overlapping");
        }

        // 5. Skapa bokningen
        Booking booking = new Booking(
                0,
                vehicleId,
                date,
                startTime,
                durationMinutes,
                description
        );

        booking.setServices(new ArrayList<>(services));
        booking.setMechanicId(mechanicId);

        // 6. Spara tjänsternas priser vid bokningstillfället
        for (ServiceItem service : services) {
            booking.addFrozenPrice(new InvoiceLine(
                    service.getId(),
                    service.getName(),
                    service.getPrice()
            ));
        }

        // 7. Spara i MySQL först
        bookingRepository.save(booking, mechanicId);

        // 8. Uppdatera minnet efter lyckad lagring
        Database.getBookings().add(booking);

        return booking;
    }

    public boolean hasOverlappingBooking(
        int mechanicId, 
        LocalDate date, 
        LocalTime startTime, 
        int durationMinutes
    ) {
        LocalDateTime newStart = LocalDateTime.of(date, startTime);
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);

        List<Booking> bookings = bookingRepository.findAll();
        for( Booking booking : bookings) {

            //kontroll endast samma mekaniker
            if (booking.getMechanicId() == null || booking.getMechanicId() != mechanicId) {
                continue;
            }

            //Hoppa över bokningar som saknar tidsinformation
            if (booking.getDate() == null
                    || booking.getStartTime() == null
                    || booking.getDurationMinutes() <= 0) {
                continue;
            }

            LocalDateTime existingStart = LocalDateTime.of(
                booking.getDate(), 
                booking.getStartTime()
            );

            LocalDateTime existingEnd = existingStart.plusMinutes(booking.getDurationMinutes());
            if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                return true;
            }
        }
        return false;
    }
    
}
