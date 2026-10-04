package com.wac.autocore.repository;


import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.*;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.wac.autocore.entity.ServiceItemEntity;

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
                if (entity.getLines() != null) {
                    entity.getLines().clear();
                }
                if (entity.getLines() == null || entity.getLines().isEmpty()) {
                    List<InvoiceLineEntity> lineEntities = convertToFrozenEntity(booking.getServices(), entity);
                    entity.setLines(lineEntities);
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

    public List<InvoiceLineEntity> convertToFrozenEntity(List<ServiceItem> services, BookingEntity bookingEntity) {
        List<InvoiceLineEntity> lineEntities = new ArrayList<>();

        for (ServiceItem service : services) {
            InvoiceLineEntity lineEntity = new InvoiceLineEntity();
            lineEntity.setServiceItemId(service.getId());
            lineEntity.setServiceName(service.getName());
            lineEntity.setPrice(service.getPrice());
            lineEntity.setDiscount(0.0); // Radrabatt är 0 enligt WAC-38
            lineEntity.setFinalPrice(service.getPrice()); // finalPrice fryses här!

            lineEntity.setBooking(bookingEntity);
            lineEntities.add(lineEntity);
        }

        return lineEntities;
    }

    public List<BookingEntity> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<BookingEntity> bookings =
                        session.createQuery(
                            "select distinct b from BookingEntity b " +
                            "left join fetch b.lines " +
                            "order by b.id ",
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
                    entity.getDescription(),
                    convertToFrozenPrice(entity.getLines())
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
                Booking booking = new Booking(
                        entity.getId(),
                        entity.getVehicleId(),
                        entity.getDate(),
                        entity.getStartTime(),
                        entity.getDurationMinutes(),
                        entity.getDescription(),
                        convertToFrozenPrice(entity.getLines())
                );

                return booking;
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    private List<InvoiceLine> convertToFrozenPrice(List<InvoiceLineEntity> entityLines) {
        List<InvoiceLine> domainLines = new ArrayList<>();
        if (entityLines == null) return domainLines;

        boolean alreadyExists = false;
        for (InvoiceLineEntity lineEntity : entityLines) {
            for (InvoiceLine exist : domainLines) {
                if(exist.getServiceItemId() == lineEntity.getServiceItemId()) {
                    alreadyExists = true;
                    break;
                }
            }
            if(!alreadyExists) {
                InvoiceLine line = new InvoiceLine(
                        lineEntity.getServiceItemId(),
                        lineEntity.getServiceName(),
                        lineEntity.getPrice()
                );
                line.setId(lineEntity.getId());
                line.setDiscount(lineEntity.getDiscount());

                domainLines.add(line);
            }
        }
        return domainLines;
    }

//    private List<ServiceItem> getServiceItems(BookingEntity entity) {
//        List<ServiceItem> serviceList = new ArrayList<>();
//        for (ServiceItemEntity serviceItems : entity.getServices()) {
//            ServiceItem service = new ServiceItem(
//                    serviceItems.getId(),
//                    serviceItems.getName(),
//                    serviceItems.getDescription(),
//                    serviceItems.getPrice(),
//                    serviceItems.getEstimatedMinutes()
//            );
//            serviceList.add(service);
//        }
//        return serviceList;
//    }

    public boolean hasOverlappingBooking(
        int mechanicId,
        LocalDate date,
        LocalTime startTime,
        int durationMinutes) {

        return hasOverlappingBooking(
                mechanicId, date, startTime, durationMinutes, null
        );
    }

    // Används vid redigering för att undanta den egna bokningen.
    public boolean hasOverlappingBooking(
            int mechanicId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            Integer excludedBookingId) {

    LocalDateTime newStart =
            LocalDateTime.of(date, startTime);

    LocalDateTime newEnd =
            newStart.plusMinutes(durationMinutes);

    for (BookingEntity entity : findAll()) {

        // Jämför inte bokningen med sig själv.
        if (excludedBookingId != null
                && entity.getId() == excludedBookingId.intValue()) {
            continue;
        }

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
    public int updateBookingServices(
            int bookingId,
            List<Integer> serviceIds) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                BookingEntity booking =
                        session.get(BookingEntity.class, bookingId);

                if (booking == null) {
                    throw new IllegalArgumentException(
                            "editBookingServices.notFound"
                    );
                }

                // Endast bokningar där arbetet inte har börjat får ändras.
                if (!"BOOKED".equals(booking.getStatus())
                        && !"WORK_ORDER_CREATED".equals(booking.getStatus())) {
                    throw new IllegalArgumentException(
                            "editBookingServices.locked"
                    );
                }

                List<WorkOrderEntity> workOrders = session.createQuery(
                        "from WorkOrderEntity where bookingId = :bookingId",
                        WorkOrderEntity.class
                ).setParameter("bookingId", bookingId).getResultList();

                for (WorkOrderEntity order : workOrders) {
                    if (!"CREATED".equals(order.getStatus())) {
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

                List<ServiceItemEntity> services = new ArrayList<>();
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

                    ServiceItemEntity service =
                            session.get(ServiceItemEntity.class, serviceId);

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
                            totalMinutes, service.getEstimatedMinutes()
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
                    List<BookingEntity> otherBookings = session.createQuery(
                                    "from BookingEntity "
                                            + "where mechanicId = :mechanicId "
                                            + "and id <> :bookingId",
                                    BookingEntity.class
                            )
                            .setParameter("mechanicId", booking.getMechanicId())
                            .setParameter("bookingId", bookingId)
                            .getResultList();

                    LocalDateTime newStart = LocalDateTime.of(
                            booking.getDate(), booking.getStartTime()
                    );
                    LocalDateTime newEnd =
                            newStart.plusMinutes(totalMinutes);

                    for (BookingEntity other : otherBookings) {
                        if (other.getDate() == null
                                || other.getStartTime() == null
                                || other.getDurationMinutes() == null
                                || other.getDurationMinutes() <= 0) {
                            continue;
                        }

                        LocalDateTime otherStart = LocalDateTime.of(
                                other.getDate(), other.getStartTime()
                        );
                        LocalDateTime otherEnd = otherStart.plusMinutes(
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

                for (WorkOrderEntity order : workOrders) {
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
