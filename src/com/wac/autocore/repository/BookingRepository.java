package com.wac.autocore.repository;


import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.entity.ServiceItemEntity;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class BookingRepository {

    public void save(BookingEntity bookingEntity) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(bookingEntity);
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    // Sparar bokningen och behåller eventuell befintlig mekaniker.
    public void save(Booking booking) {
        saveBooking(booking, null, false);
    }

    // Sparar bokningen tillsammans med vald mekaniker.
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
                // Hämtar befintlig entity så extra uppgifter bevaras.
                BookingEntity entity =
                        session.get(BookingEntity.class, booking.getId());

                boolean isNew = entity == null;

                if (isNew) {
                    entity = new BookingEntity();
                    entity.setId(booking.getId());
                }

                entity.setVehicleId(booking.getVehicleId());
                entity.setDate(booking.getDate());
                entity.setDescription(booking.getDescription());
                entity.setStatus(booking.getStatus());
                entity.setStartTime(booking.getStartTime());
                entity.setDurationMinutes(booking.getDurationMinutes());

                // Ändrar mekanikern bara när ett nytt val skickas in.
                if (updateMechanic) {
                    entity.setMechanicId(mechanicId);
                }

                if (isNew) {
                    session.save(entity);
                }

                // Befintliga entities uppdateras automatiskt av Hibernate.
                transaction.commit();

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public List<BookingEntity> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<BookingEntity> bookings =
                        session.createQuery("from BookingEntity order by id",
                                BookingEntity.class
                        ).getResultList();
                transaction.commit();
                return bookings;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }

    }

    public List<Booking> findAllBookings() {

        List<Booking> bookings = new ArrayList<>();

        for (BookingEntity entity : findAll()) {

            int durationMinutes =
            entity.getDurationMinutes() == null
                    ? 0
                    : entity.getDurationMinutes();

            Booking booking = new Booking(
                    entity.getId(),
                    entity.getVehicleId(),
                    entity.getDate(),
                    entity.getStartTime(),
                    durationMinutes,
                    entity.getDescription()
            );

            booking.setStatus(entity.getStatus());
            bookings.add(booking);
        }
        return bookings;
    }

    public Booking findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction  transaction = session.beginTransaction();
            try {
                BookingEntity entity = session.get(BookingEntity.class, id);
                transaction.commit();

                if (entity == null) {
                    return null;
                }
                Booking Booking = new Booking(
                        entity.getId(),
                        entity.getVehicleId(),
                        entity.getDate(),
                        entity.getStartTime(),
                        entity.getDurationMinutes(),
                        entity.getDescription()
                );
                List<ServiceItem> serviceList = new ArrayList<>();
                for (ServiceItemEntity serviceItems : entity.getServices()) {
                    ServiceItem service = new ServiceItem(
                            serviceItems.getId(),
                            serviceItems.getName(),
                            serviceItems.getDescription(),
                            serviceItems.getPrice(),
                            serviceItems.getEstimatedMinutes()
                    );
                    serviceList.add(service);
                }
                booking.setServices(serviceList);
                return Booking;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public boolean hasOverlappingBooking(
        int mechanicId,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes) {

    LocalDateTime newStart =
            LocalDateTime.of(date, startTime);

    LocalDateTime newEnd =
            newStart.plusMinutes(durationMinutes);

    for (BookingEntity entity : findAll()) {

        // Bokningen måste tillhöra samma mekaniker.
        if (entity.getMechanicId() == null ||
                entity.getMechanicId() != mechanicId) {
            continue;
        }

        // Äldre bokningar kan sakna tid/längd.
        if (entity.getDate() == null ||
        entity.getStartTime() == null ||
        entity.getDurationMinutes() == null ||
        entity.getDurationMinutes() <= 0) {
            continue;
        }

        LocalDateTime existingStart =
                LocalDateTime.of(
                        entity.getDate(),
                        entity.getStartTime()
                );

        LocalDateTime existingEnd =
                existingStart.plusMinutes(
                        entity.getDurationMinutes()
                );

        boolean overlaps =
                newStart.isBefore(existingEnd)
                        &&
                newEnd.isAfter(existingStart);

        if (overlaps) {
            return true;
        }
    }

    return false;
}
}
