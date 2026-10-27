package com.wac.autocore.repository;

import com.wac.autocore.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional
public class BookingRepository implements IBookingRepository{

    @PersistenceContext
    private EntityManager entityManager;
    //använder entitymanager i andra metoder, ersätta SessionFactory med den
    //all hibernate ska bort
    //transaction sköter commit och rollback nu

        @Override
        public Booking save(Booking booking) {
        entityManager.persist(booking);
        return booking;}

        @Override
        public void update(Booking booking){
            entityManager.merge(booking);
        }

        // Sparar bokningen tillsammans med vald mekaniker.
        @Override 
        public Booking save(Booking booking, int mechanicId) {
            Mechanic mechanic = entityManager.find(Mechanic.class, mechanicId);
            if(mechanic != null){
                booking.setMechanicId(mechanicId);
            } else { throw new IllegalArgumentException("Mechanic not found"); }
                entityManager.persist(booking);
            return booking;
        }

        @Override
        public void updateBookingAndOrder(List<WorkOrder> orderList, Booking booking){
            entityManager.merge(booking);
            if(orderList != null) {
                for(WorkOrder workOrder : orderList){
                    entityManager.merge(workOrder);
                }
            }
        }

        //Används den inte?
        private Booking saveBooking(
                Booking booking,
                Integer mechanicId,
                boolean updateMechanic
        ) {
                        // Hämtar befintlig bokning så extra uppgifter bevaras.
                        Booking existingBooking = null;


                        //Hämta bara en befintlig booking om den har ett ID
                        if (booking.getId() > 0) {
                            existingBooking = entityManager.find(Booking.class, booking.getId());
                            if (existingBooking == null) {
                                throw new IllegalArgumentException("Booking not found");
                            }

                            boolean isNew = (existingBooking == null);

                            if (isNew) {
                                existingBooking = new Booking();
                            }

                            existingBooking.setVehicleId(booking.getVehicleId());
                            existingBooking.setDate(booking.getDate());
                            existingBooking.setDescription(booking.getDescription());
                            existingBooking.setStatus(booking.getStatus());
                            existingBooking.setStartTime(booking.getStartTime());
                            existingBooking.setDurationMinutes(
                                    booking.getDurationMinutes()
                            );


                            List<ServiceItem> serviceItems = new ArrayList<>();
                            for (ServiceItem service : booking.getServices()) {
                                ServiceItem serviceItem =
                                        entityManager.find(ServiceItem.class, service.getId());
                                if (serviceItem != null) {
                                    serviceItems.add(serviceItem);
                                }
                            }
                            existingBooking.setServices(serviceItems);

                            /*
                             * WAC-39:
                             * När bokningen skapas sparas en snapshot av tjänsternas
                             * namn och pris. Snapshoten ska inte ändras när priset i
                             * ServiceItem senare ändras.
                             */
                            if (isNew) {
                                List<InvoiceLine> incomingFrozen = booking.getFrozenPrice();
                                if (incomingFrozen == null || incomingFrozen.isEmpty()) {
                                    existingBooking.setFrozenPrice(createFrozenPrices(serviceItems));
                                } else {
                                    existingBooking.setFrozenPrice(new ArrayList<>(incomingFrozen));
                                }
                            }

                            // Ändrar mekanikern bara när ett nytt val skickas in.
                            // Uppdatera mekanikern när ett nytt val skickas in.
                            if (updateMechanic) {
                                existingBooking.setMechanicId(mechanicId);
                            } else if (isNew) {
                                existingBooking.setMechanicId(booking.getMechanicId());
                            }

                            if (isNew) {
                                entityManager.persist(existingBooking);
                            }

                            //Synkronisera ID från det sparade objektet
                            booking.setId(existingBooking.getId());
                        }
                        return booking;
        }

        @Override 
        public List<Booking> findAll() {

              return entityManager.createQuery(
                                        "select distinct b from Booking b " +
                                                "left join fetch b.services " +
                                                "order by b.id",
                                        Booking.class
                                ).getResultList();
        }

        @Override
        public Booking findById(int id) {
            return entityManager.find(Booking.class, id);
        }

        @Override
        public Booking findByIdAndServices(int id){
            List<Booking> bookingList = entityManager.createQuery(
                    "SELECT b FROM Booking b LEFT JOIN FETCH b.services WHERE b.id = :id",
                    Booking.class)
                    .setParameter("id", id)
                    .getResultList();
            return bookingList.getFirst();
        }

        @Override
        public List<Booking> findOtherBookingsByMechanic(int mechanicId, int bookingId) {
            return entityManager.createQuery(
                    "FROM Booking WHERE mechanicId = :mechanicId and id <> :bookingId",
                    Booking.class)
                    .setParameter("mechanicId", mechanicId)
                    .setParameter("bookingId", bookingId)
                    .getResultList();
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