package com.wac.autocore.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.IBookingRepository;
import com.wac.autocore.repository.IServiceItemRepository;
import com.wac.autocore.repository.IWorkOrderRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingService.class);
    private final IBookingRepository bookingRepository;
    private final IWorkOrderRepository workOrderRepository;
    private final IServiceItemRepository serviceItemRepository;

    public BookingService(IBookingRepository bookingRepository, IWorkOrderRepository workOrderRepository,
                          IServiceItemRepository serviceItemRepository) {
        this.bookingRepository = bookingRepository;
        this.workOrderRepository = workOrderRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public List<Booking> getAllBookings(){
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(int id ) {
        return Optional.ofNullable(bookingRepository.findById(id));
    }

    
    public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds) {

        // Spara först i databasen.
        int totalMinutes = bookingRepository.updateBookingServices(
                bookingId,
                serviceIds
        );

        // Uppdatera minnesdata efter lyckat sparande.
        List<ServiceItem> updatedServices = new ArrayList<>();

        for (Integer serviceId : serviceIds) {
            for (ServiceItem service : Database.getServiceItems()) {
                if (service.getId() == serviceId
                        && updatedServices.stream()
                            .noneMatch(item -> item.getId() == serviceId)) {

                    updatedServices.add(service);
                    break;
                }
            }
        }

        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == bookingId) {
                booking.setServices(new ArrayList<>(updatedServices));
                booking.setDurationMinutes(totalMinutes);

                List<InvoiceLine> frozenPrices = new ArrayList<>();

                for (ServiceItem service : updatedServices) {
                    frozenPrices.add(new InvoiceLine(
                            service.getId(),
                            service.getName(),
                            service.getPrice()
                    ));
                }

                booking.setFrozenPrice(frozenPrices);
            }
        }

        for (com.wac.autocore.model.WorkOrder order
                : Database.getWorkOrders()) {

            if (order.getBookingId() == bookingId
                    && order.getStatus()
                        == com.wac.autocore.model.WorkOrderStatus.CONFIRMED) {

                order.setServiceItemIds(
                        updatedServices.stream()
                                .map(ServiceItem::getId)
                                .toList()
                );
            }
        }

        return totalMinutes;
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

        LOGGER.info("Booking {} created for vehicle {}", booking.getId(), vehicleId);

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
        boolean result = BookingOverlapChecker.hasOverlap(bookings, mechanicId, newStart, newEnd);

        return result;
    }

    public List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems) {

        List<InvoiceLine> frozenPrices = new ArrayList<>();
        for (ServiceItem service : serviceItems) {
            InvoiceLine frozenLine = new InvoiceLine(
                    service.getId(),
                    service.getName(),
                    service.getPrice() // Sparar priset inför faktureringen
            );
            frozenPrices.add(frozenLine);
        }
        return frozenPrices;
    }
}
