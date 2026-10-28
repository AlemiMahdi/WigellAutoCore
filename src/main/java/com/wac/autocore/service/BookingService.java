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

public class BookingService {

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
        boolean result = BookingOverlapChecker.hasOverlap(bookings, mechanicId, newStart, newEnd);

        return result;
    }

    public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds
    ) {
        Booking booking = bookingRepository.findByIdAndServices(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException(
                    "editBookingServices.notFound"
            );
        }

        // Endast bokningar där arbetet inte har börjat får ändras.
        if (!"BOOKED".equals(booking.getStatus())
                && !"WORK_ORDER_CREATED".equals(
                booking.getStatus()
        )) { throw new IllegalArgumentException(
                    "editBookingServices.locked");
        }

        List<WorkOrder> workOrders = workOrderRepository.findByBookingId(bookingId);
        for (WorkOrder order : workOrders) {
            if (!order.getStatus().allowsServiceChanges()) {
                throw new IllegalArgumentException(
                        "editBookingServices.locked"
                );
            }
        }

        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "editBookingServices.selectService"
            );
        }

        List<ServiceItem> services = new ArrayList<>();
        List<Integer> uniqueIds = new ArrayList<>();

        int totalMinutes = 0;

        for (Integer serviceId : serviceIds) {

            if (serviceId == null) {
                throw new IllegalArgumentException(
                        "editBookingServices.serviceMissing"
                );
            }

            if (uniqueIds.contains(serviceId)) {
                continue;
            }

            ServiceItem service = serviceItemRepository.findById(serviceId);
            if (service == null) {
                throw new IllegalArgumentException(
                        "editBookingServices.serviceMissing");
            }

            if (service.getEstimatedMinutes() <= 0) {
                throw new IllegalArgumentException(
                        "editBookingServices.invalidDuration");
            }

            services.add(service);
            uniqueIds.add(serviceId);

            totalMinutes = Math.addExact(
                    totalMinutes,
                    service.getEstimatedMinutes()
            );
        }

        if (booking.getDate() == null
                || booking.getStartTime() == null) {

            throw new IllegalArgumentException(
                    "editBookingServices.missingTime");
        }

        // Kontrollerar andra bokningar i samma session.
        if (booking.getMechanicId() != null) {

            List<Booking> otherBookings = bookingRepository.findOtherBookingsByMechanic(booking.getMechanicId(), booking.getId());

            LocalDateTime newStart =
                    LocalDateTime.of(
                            booking.getDate(),
                            booking.getStartTime());

            LocalDateTime newEnd =
                    newStart.plusMinutes(totalMinutes);

            if (BookingOverlapChecker.hasOverlap(otherBookings, booking.getMechanicId(), newStart, newEnd)) {
                throw new IllegalArgumentException("editBookingServices.overlap");
            }
        }

        // Alla ändringar sparas i samma transaktion.
        booking.getServices().clear();
        booking.getServices().addAll(services);
        booking.setDurationMinutes(totalMinutes);

        /*
         * Bokningens tjänster får ändras innan arbetet har börjat.
         * Därför uppdateras även historical-price-snapshoten.
         */
        booking.getFrozenPrice().clear();
        booking.getFrozenPrice().addAll(createFrozenPrices(services));

        for (WorkOrder order : workOrders) {
            order.getServiceItemIds().clear();
            order.getServiceItemIds().addAll(uniqueIds);
        }
        bookingRepository.updateBookingAndOrder(workOrders, booking);
//        bookingRepository.update(booking);
//        for(WorkOrder order : workOrders) {
//            workOrderRepository.save(order);
//        }
        return totalMinutes;
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
