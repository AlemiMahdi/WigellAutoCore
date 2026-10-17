package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;

import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository implements IBookingRepository{

        @Override 
        public void save(Booking booking) {
                saveBooking(booking, null, false);
        }

        // Sparar bokningen tillsammans med vald mekaniker.
        @Override 
        public void save(Booking booking, int mechanicId) {
                saveBooking(booking, mechanicId, true);
        }

        private void saveBooking(
                Booking booking,
                Integer mechanicId,
                boolean updateMechanic
        ) {

                try (Session session =
                        HibernateUtil.getSessionFactory().openSession()) {

                Transaction transaction = session.beginTransaction();

                try {

                        // Hämtar befintlig bokning så extra uppgifter bevaras.
                        Booking existingBooking = null;

                        //Hämta bara en befintlig booking om den har ett ID
                        if (booking.getId() > 0) {
                                existingBooking = session.get(
                                        Booking.class, 
                                        booking.getId()
                                );
                        }

                        // En bokning med ett befintligt ID måste finnas i databasen.
                        if (booking.getId() > 0 && existingBooking == null) {
                        throw new IllegalArgumentException(
                                "Booking not found: " + booking.getId()
                        );
                        }

                        boolean isNew = existingBooking == null;

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

                        // Hämtar ServiceItem från samma Hibernate-session.
                        List<ServiceItem> serviceItems = new ArrayList<>();

                        for (ServiceItem service : booking.getServices()) {

                        ServiceItem serviceItem =
                                session.get(
                                        ServiceItem.class,
                                        service.getId()
                                );

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
                        
                        List<InvoiceLine> frozenPrices = booking.getFrozenPrice();
                                if (frozenPrices == null || frozenPrices.isEmpty()) {
                                        frozenPrices = new ArrayList<>();
                                        for(ServiceItem service : serviceItems) {
                                                frozenPrices.add( new InvoiceLine(
                                                        service.getId(),
                                                        service.getName(),
                                                        service.getPrice()
                                                        )
                                                );
                                        }
                                }
                                existingBooking.setFrozenPrice(new ArrayList<>(frozenPrices));
                        
                        }

                        // Ändrar mekanikern bara när ett nytt val skickas in.
                        // Uppdatera mekanikern när ett nytt val skickas in.
                        if (updateMechanic) {
                        existingBooking.setMechanicId(mechanicId);
                        }
                        // Om bokningen är ny, kopiera dess befintliga mechanicId.
                        else if (isNew) {
                        existingBooking.setMechanicId(booking.getMechanicId());
                        }

                        if (isNew) {
                        session.save(existingBooking);
                        }

                        // Befintliga objekt uppdateras automatiskt av Hibernate.
                        transaction.commit();

                        //Synkronisera ID från det sparade objektet
                        booking.setId(existingBooking.getId());

                } catch (RuntimeException exception) {

                        if (transaction.isActive()) {
                        transaction.rollback();
                        }

                        throw exception;
                }
                }
        }

        @Override 
        public List<Booking> findAll() {

                try (Session session =
                        HibernateUtil.getSessionFactory().openSession()) {

                Transaction transaction = session.beginTransaction();

                try {

                        List<Booking> bookings =
                                session.createQuery(
                                        "select distinct b from Booking b " +
                                                "left join fetch b.services " +
                                                "order by b.id",
                                        Booking.class
                                ).getResultList();

                        // frozenPrice är EAGER och läses tillsammans med Booking.
                        for (Booking booking : bookings) {
                        booking.getFrozenPrice().size();
                        }

                        transaction.commit();

                        return bookings;

                } catch (RuntimeException exception) {

                        if (transaction.isActive()) {
                        transaction.rollback();
                        }

                        throw exception;
                }
                }
        }

        public List<Booking> findAllBookings() {
                return findAll();
        }

        @Override
        public Booking findById(int id) {

                try (Session session =
                        HibernateUtil.getSessionFactory().openSession()) {

                Transaction transaction = session.beginTransaction();

                try {

                        Booking booking =
                                session.get(Booking.class, id);

                        if (booking != null) {
                        booking.getServices().size();
                        booking.getFrozenPrice().size();
                        }

                        transaction.commit();

                        return booking;

                } catch (RuntimeException exception) {

                        if (transaction.isActive()) {
                        transaction.rollback();
                        }

                        throw exception;
                }
                }
        }

        public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds
    ) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                Booking booking =
                        session.get(Booking.class, bookingId);

                if (booking == null) {
                    throw new IllegalArgumentException(
                            "editBookingServices.notFound"
                    );
                }

                // Endast bokningar där arbetet inte har börjat får ändras.
                if (!"BOOKED".equals(booking.getStatus())
                        && !"WORK_ORDER_CREATED".equals(
                                booking.getStatus()
                        )) {

                    throw new IllegalArgumentException(
                            "editBookingServices.locked"
                    );
                }

                List<WorkOrder> workOrders =
                        session.createQuery(
                                "from WorkOrder " +
                                        "where bookingId = :bookingId",
                                WorkOrder.class
                        )
                        .setParameter("bookingId", bookingId)
                        .getResultList();

                for (WorkOrder order : workOrders) {

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

                    ServiceItem service =
                            session.get(
                                    ServiceItem.class,
                                    serviceId
                            );

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
                            session.createQuery(
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

                // Alla ändringar sparas i samma transaktion.
                booking.getServices().clear();
                booking.getServices().addAll(services);
                booking.setDurationMinutes(totalMinutes);

                /*
                 * Bokningens tjänster får ändras innan arbetet har börjat.
                 * Därför uppdateras även historical-price-snapshoten.
                 */
                List<InvoiceLine> frozenPrices = new ArrayList<>();

                for (ServiceItem service : services) {

                    InvoiceLine frozenLine = new InvoiceLine(
                            service.getId(),
                            service.getName(),
                            service.getPrice()
                    );

                    frozenPrices.add(frozenLine);
                }

                booking.getFrozenPrice().clear();
                booking.getFrozenPrice().addAll(frozenPrices);

                for (WorkOrder order : workOrders) {
                    order.getServiceItemIds().clear();
                    order.getServiceItemIds().addAll(uniqueIds);
                }

                transaction.commit();

                return totalMinutes;

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }
}