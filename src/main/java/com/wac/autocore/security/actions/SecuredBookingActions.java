package com.wac.autocore.security.actions;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.BookingService;

@Component 
public class SecuredBookingActions {
    
    private final BookingService bookingService;

    public SecuredBookingActions( BookingService bookingService) {
        this.bookingService = bookingService;
    }

    //Alla inloggade användare får visa bokningar
    @PreAuthorize ("isAuthenticated()")
    public List<Booking> getAllBookings(){
        return bookingService.getAllBookings();
    }

    //Endast ADMIN och WORKSHOP får skapa bokningar
    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public Optional<Booking> getBookingById(int id){
        return bookingService.getBookingById(id);
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public Booking createBooking(
        int vehicleId,
        LocalDate date,
        LocalTime starTime,
        int durationMinutes,
        String description,
        List<ServiceItem> services,
        int mechanicId
    ){
        return bookingService.createBooking(vehicleId, date, starTime, durationMinutes, description, services, mechanicId);
    }

   // Endast ADMIN och WORKSHOP får redigera bokningar
    @PreAuthorize("hasAnyRole('ADMIN', 'WORKSHOP')")
    public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds) {

        return bookingService.updateBookingServices(
                bookingId,
                serviceIds
        );
    }
}
