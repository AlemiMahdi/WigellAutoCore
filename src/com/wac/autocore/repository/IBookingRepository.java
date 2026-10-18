package com.wac.autocore.repository;

import java.util.List;

import com.wac.autocore.model.Booking;

public interface IBookingRepository {
    
    void save( Booking booking );
    void save( Booking booking, int mechanicId );
    List<Booking> findAll();
    Booking findById( int id );
    
}
