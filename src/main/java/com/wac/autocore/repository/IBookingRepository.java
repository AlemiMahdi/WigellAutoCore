package com.wac.autocore.repository;

import java.util.List;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;

public interface IBookingRepository {
    
    Booking save(Booking booking);
    Booking save(Booking booking, int mechanicId);
    List<Booking> findAll();
    Booking findById(int id);
    int updateBookingServices(int bookingId, List<Integer> serviceIds);
    List<Booking> findOtherBookingsByMechanic(int mechanicId, int bookingId);
    Booking findByIdAndServices(int id);
    void update(Booking booking);
    void updateBookingAndOrder(List<WorkOrder> orderList, Booking booking);
}
