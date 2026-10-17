package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.IMechanicRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;

import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MechanicService {

    private final IMechanicRepository mechanicRepository;
    private final Supplier<List<Booking>> bookingSource;

    public MechanicService(IMechanicRepository mechanicRepository) {
        this(
                mechanicRepository,
                () -> new ArrayList<>(Database.getBookings())
        );
    }

    public MechanicService(
            IMechanicRepository mechanicRepository,
            Supplier<List<Booking>> bookingSource) {

        this.mechanicRepository = Objects.requireNonNull(
                mechanicRepository, "Mechanic repository is required"
        );
        this.bookingSource = Objects.requireNonNull(
                bookingSource, "Booking source is required"
        );
    }

    public void save(Mechanic mechanic) {
        Objects.requireNonNull(mechanic, "Mechanic is required");
        mechanicRepository.save(mechanic);
    }

    public List<Mechanic> findAll() {
        return mechanicRepository.findAll();
    }

    public List<Mechanic> getLoadedMechanics() {
        return new ArrayList<>(Database.getMechanics());
    }

    public List<Booking> getSchedule(int mechanicId) {
        List<Booking> bookings = bookingSource.get();

        Map<Integer, Integer> mechanicByBooking = new HashMap<>();

        for (Booking booking : bookings) {
            if (booking.getMechanicId() != null) {
                mechanicByBooking.put(
                        booking.getId(), booking.getMechanicId()
                );
            }
        }

        for (WorkOrder order : Database.getWorkOrders()) {
            mechanicByBooking.put(
                    order.getBookingId(), order.getMechanicId()
            );
        }

        Comparator<Booking> scheduleOrder = Comparator
                .comparing(
                        Booking::getDate,
                        Comparator.nullsLast(
                                Comparator.<LocalDate>naturalOrder()
                        )
                )
                .thenComparing(booking ->
                        booking.getStartTime() == null
                                ? LocalTime.MIN
                                : booking.getStartTime());

        return bookings.stream()
                .filter(booking -> Integer.valueOf(mechanicId).equals(
                        mechanicByBooking.get(booking.getId())
                ))
                .sorted(scheduleOrder)
                .collect(Collectors.toList());
    }

}