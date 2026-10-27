package com.wac.autocore.repository;

import java.util.List;

import com.wac.autocore.model.Booking;

public interface IBookingRepository {
    
    Booking save( Booking booking );
    Booking save( Booking booking, int mechanicId );
    List<Booking> findAll();
    Booking findById( int id );
    void update(Booking booking);
}
