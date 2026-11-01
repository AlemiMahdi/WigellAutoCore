package com.wac.autocore.standalone;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.IBookingRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Bokningsrepo som sparar i en lista i minnet i stället för MySQL.
 * Används av det fristående programmet (WAC-106) så att BookingService
 * kan köras utan Spring och utan databas.
 * Ingen @Repository här, annars hittar spring två repon i Main 1
 */
public class InMemoryBookingRepository implements IBookingRepository {

    private final List<Booking> bookings = new ArrayList<>();

    //Ersätter databasens AUTO_INCREMENT
    private int nextId = 1;

    // Ger bokningen ett id och lägger den i listan
    @Override
    public Booking save(Booking booking) {
        booking.setId(nextId++);
        bookings.add(booking);
        return booking;
    }

    // Sätter mekaniker och sparar sedan som vanligt
    @Override
    public Booking save(Booking booking, int mechanicId) {
        booking.setMechanicId(mechanicId);
        return save(booking);
    }

    // Returnerar en kopia så att listan inte kan ändras utifrån
    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(bookings);
    }

    // Metoderna nedan används inte av det fristående programmet.
    // De finns bara för att interfacet kräver dem.
    @Override
    public Booking findById(int id) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }


    @Override
    public List<Booking> findOtherBookingsByMechanic(int mechanicId, int bookingId) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }

    @Override
    public Booking findByIdAndServices(int id) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }

    @Override
    public void update(Booking booking) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }

    @Override
    public void updateBookingAndOrder(List<WorkOrder> orderList, Booking booking) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }

    @Override
    public int updateBookingServices(int bookingId, List<Integer> serviceIds) {
        throw new UnsupportedOperationException("Not used in standalone program");
    }
}
