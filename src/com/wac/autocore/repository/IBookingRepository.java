package com.wac.autocore.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.wac.autocore.model.Booking;

public interface IBookingRepository {
    
    public void save( Booking booking );
    public void save( Booking booking, int mechanicId );
    public List<Booking> findAll();
    public Booking findById( int id );
    
    boolean hasOverlappingBooking(
        int mechanicId,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes
    );
    
}
