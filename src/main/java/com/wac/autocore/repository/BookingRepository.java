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
            return entityManager.createQuery(
                    "SELECT DISTINCT b FROM Booking b " +
                    "LEFT JOIN FETCH b.services " +
                    "WHERE b.id = :id",
                    Booking.class
            )
            .setParameter("id", id)
            .getResultStream()
            .findFirst()
            .orElse(null);
        }

        @Override 
        public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds
    ) {
                Booking booking = entityManager.find(Booking.class, bookingId);
                if (booking == null) {
                    throw new IllegalArgumentException(
                            "editBookingServices.notFound"
                    );
                }

                // Endast bokningar d├ñr arbetet inte har b├Ârjat f├Ñr ├ñndras.
                if (!"BOOKED".equals(booking.getStatus())
                        && !"WORK_ORDER_CREATED".equals(
                                booking.getStatus()
                        )) {

                    throw new IllegalArgumentException(
                            "editBookingServices.locked"
                    );
                }

                List<WorkOrder> workOrders =
                        entityManager.createQuery(
                                "from WorkOrder " +
                                        "where bookingId = :bookingId",
                                WorkOrder.class
                        )
                        .setParameter("bookingId", bookingId)
                        .getResultList();

                for (WorkOrder order : workOrders) {
                    System.out.println("WorkOrder ID: " + order.getId());
                    System.out.println("WorkOrder status: " + order.getStatus());

                    if (order.getStatus() != WorkOrderStatus.CONFIRMED) {
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

                    ServiceItem service = entityManager.find(ServiceItem.class, serviceId);
                    if (service == null) {
                        throw new IllegalArgumentException(
                                "editBookingServices.serviceMissing"
                        );
                    }

                    if (service.getEstimatedMinutes() <= 0) {
                        throw new IllegalArgumentException(
                                "editBookingServices.invalidDuration"
                        );
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
                            "editBookingServices.missingTime"
                    );
                }

                // Kontrollerar andra bokningar i samma session.
                if (booking.getMechanicId() != null) {

                    List<Booking> otherBookings =
                            entityManager.createQuery(
                                    "from Booking " +
                                            "where mechanicId = :mechanicId " +
                                            "and id <> :bookingId",
                                    Booking.class
                            )
                            .setParameter(
                                    "mechanicId",
                                    booking.getMechanicId()
                            )
                            .setParameter(
                                    "bookingId",
                                    bookingId
                            )
                            .getResultList();

                    LocalDateTime newStart =
                            LocalDateTime.of(
                                    booking.getDate(),
                                    booking.getStartTime()
                            );

                    LocalDateTime newEnd =
                            newStart.plusMinutes(totalMinutes);

                    for (Booking other : otherBookings) {

                        if (other.getDate() == null
                                || other.getStartTime() == null
                                || other.getDurationMinutes() <= 0) {
                            continue;
                        }

                        LocalDateTime otherStart =
                                LocalDateTime.of(
                                        other.getDate(),
                                        other.getStartTime()
                                );

                        LocalDateTime otherEnd =
                                otherStart.plusMinutes(
                                        other.getDurationMinutes()
                                );

                        if (newStart.isBefore(otherEnd)
                                && newEnd.isAfter(otherStart)) {

                            throw new IllegalArgumentException(
                                    "editBookingServices.overlap"
                            );
                        }
                    }
                }

                // Alla ├ñndringar sparas i samma transaktion.
                booking.getServices().clear();
                booking.getServices().addAll(services);
                booking.setDurationMinutes(totalMinutes);

                /*
                 * Bokningens tj├ñnster f├Ñr ├ñndras innan arbetet har b├Ârjat.
                 * D├ñrf├Âr uppdateras ├ñven historical-price-snapshoten.
                 */
                booking.getFrozenPrice().clear();
                booking.getFrozenPrice().addAll(createFrozenPrices(services));

                for (WorkOrder order : workOrders) {
                    order.getServiceItemIds().clear();
                    order.getServiceItemIds().addAll(uniqueIds);
                }
                return totalMinutes;
    }
    
    @Override
    public Booking findByIdAndServices(int id) {
        List<Booking> bookingList = entityManager.createQuery(
                "SELECT DISTINCT b FROM Booking b " +
                "LEFT JOIN FETCH b.services WHERE b.id = :id",
                Booking.class
        )
        .setParameter("id", id)
        .getResultList();

        return bookingList.isEmpty() ? null : bookingList.getFirst();
    }

    @Override
    public List<Booking> findOtherBookingsByMechanic(
            int mechanicId, int bookingId) {

        return entityManager.createQuery(
                "FROM Booking WHERE mechanicId = :mechanicId " +
                "AND id <> :bookingId",
                Booking.class
        )
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