package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrderStatus;
import org.hibernate.LockMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class WorkOrderRepository {

    public void save(WorkOrder workOrder) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                session.saveOrUpdate(workOrder);

                transaction.commit();

            } catch (RuntimeException e) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        }
    }

    // Sparar bokningen och utkastet i samma transaktion (WAC-63)
    // Antingen sparas båda, eller ingen av dem.
    public void saveWithBooking(WorkOrder workOrder, Booking booking) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.save(booking);
                session.save(workOrder);

                transaction.commit();
            } catch (RuntimeException e) {
                 if (transaction.isActive()) {
                     transaction.rollback();
                 }
                 throw e;
            }
        }
    }

    public List<WorkOrder> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<WorkOrder> workOrders =
                        session.createQuery(
                                "from WorkOrder order by id",
                                WorkOrder.class
                        ).getResultList();

                transaction.commit();

                return workOrders;

            } catch (RuntimeException e) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        }
    }

    public List<WorkOrder> findAllWorkOrders() {
        return findAll();
    }

    public void updateDraft(
            WorkOrder draft,
            Booking booking,
            List<Integer> serviceIds,
            int mechanicId,
            LocalDate plannedDate,
            int minutes,
            BigDecimal price,
            String instructions,
            String comments
    ) {
        if (draft == null || draft.getStatus() != WorkOrderStatus.DRAFT) {
            throw new IllegalArgumentException("editWorkOrderDraft.onlyDraft");
        }

        if (booking == null || booking.getId() != draft.getBookingId()) {
            throw new IllegalArgumentException("editWorkOrderDraft.bookingMissing");
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("editWorkOrderDraft.invalidMinutes");
        }

        BigDecimal normalizedPrice = price;

        if (price != null) {
            try {
                normalizedPrice = price.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("editWorkOrderDraft.invalidPrice");
            }

            if (normalizedPrice.signum() < 0
                    || normalizedPrice.compareTo(
                    new BigDecimal("9999999999.99")) > 0) {
                throw new IllegalArgumentException("editWorkOrderDraft.invalidPrice");
            }
        }

        String savedInstructions = instructions == null ? "" : instructions.trim();
        String savedComments = comments == null ? "" : comments.trim();

        if (savedInstructions.length() > 4000 || savedComments.length() > 4000) {
            throw new IllegalArgumentException("editWorkOrderDraft.textTooLong");
        }

        List<Integer> selectedIds =
                new ArrayList<>(new LinkedHashSet<>(serviceIds));

        List<ServiceItem> savedServices = new ArrayList<>();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                WorkOrder savedDraft = session.get(
                        WorkOrder.class, draft.getId(), LockMode.PESSIMISTIC_WRITE
                );

                if (savedDraft == null
                        || savedDraft.getStatus() != WorkOrderStatus.DRAFT) {
                    throw new IllegalArgumentException(
                            "editWorkOrderDraft.onlyDraft"
                    );
                }

                Booking savedBooking = session.get(
                        Booking.class,
                        savedDraft.getBookingId(),
                        LockMode.PESSIMISTIC_WRITE
                );

                if (savedBooking == null || savedBooking.getId() != booking.getId()) {
                    throw new IllegalArgumentException(
                            "editWorkOrderDraft.bookingMissing"
                    );
                }

                if (mechanicId != 0
                        && session.get(Mechanic.class, mechanicId) == null) {
                    throw new IllegalArgumentException(
                            "editWorkOrderDraft.mechanicMissing"
                    );
                }

                for (Integer serviceId : selectedIds) {
                    ServiceItem service = serviceId == null
                            ? null
                            : session.get(ServiceItem.class, serviceId);

                    if (service == null) {
                        throw new IllegalArgumentException(
                                "editWorkOrderDraft.serviceMissing"
                        );
                    }

                    savedServices.add(service);
                }

                savedDraft.getServiceItemIds().clear();
                savedDraft.getServiceItemIds().addAll(selectedIds);
                savedDraft.setMechanicId(mechanicId);
                savedDraft.setEstimatedPrice(normalizedPrice);
                savedDraft.setCustomerInstructions(savedInstructions);
                savedDraft.setDraftComments(savedComments);

                savedBooking.setDate(plannedDate);
                savedBooking.setDurationMinutes(minutes);
                savedBooking.setMechanicId(mechanicId == 0 ? null : mechanicId);
                savedBooking.getServices().clear();
                savedBooking.getServices().addAll(savedServices);

                // Status, problembeskrivning och historiska priser ändras inte.
                transaction.commit();

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }

        // Uppdaterar appens objekt först efter lyckad commit.
        draft.setServiceItemIds(new ArrayList<>(selectedIds));
        draft.setMechanicId(mechanicId);
        draft.setEstimatedPrice(normalizedPrice);
        draft.setCustomerInstructions(savedInstructions);
        draft.setDraftComments(savedComments);

        booking.setDate(plannedDate);
        booking.setDurationMinutes(minutes);
        booking.setMechanicId(mechanicId == 0 ? null : mechanicId);
        booking.setServices(new ArrayList<>(savedServices));
    }
}