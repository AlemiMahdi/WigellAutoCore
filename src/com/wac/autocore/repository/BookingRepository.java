package com.wac.autocore.repository;


import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Booking;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

            Booking booking = new Booking(
                    entity.getId(),
                    entity.getVehicleId(),
                    entity.getDate(),
                    entity.getDescription()
            );

            booking.setStatus(entity.getStatus());
            bookings.add(booking);
        }
        return bookings;
    }
}
