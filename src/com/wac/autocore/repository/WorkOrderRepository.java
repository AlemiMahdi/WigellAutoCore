package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.*;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

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
                workOrder.setBookingId(booking.getId());
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

    public List<String> confirmDraft(int workOrderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                List<String> errors = new ArrayList<>();

                WorkOrder workOrder = session.get(
                        WorkOrder.class,
                        workOrderId,
                        LockMode.PESSIMISTIC_WRITE
                );

                if (workOrder == null) {
                    errors.add("confirmWorkOrder.orderMissing");
                } else if (workOrder.getStatus() != WorkOrderStatus.DRAFT) {
                    errors.add("confirmWorkOrder.onlyDraft");
                }

                if (!errors.isEmpty()) {
                    transaction.rollback();
                    return errors;
                }

                if (workOrder.getType() == null) {
                    errors.add("confirmWorkOrder.typeMissing");
                }

                if (workOrder.getType() == WorkOrderTypeEnum.COMPLAINT) {
                    Integer originalId = workOrder.getOriginalWorkOrderId();

                    if (originalId == null) {
                        errors.add("confirmWorkOrder.originalRequired");
                    } else if (originalId.intValue() == workOrder.getId()) {
                        errors.add("confirmWorkOrder.originalSelfReference");
                    } else {
                        WorkOrder original = session.get(WorkOrder.class, originalId);

                        if (original == null) {
                            errors.add("confirmWorkOrder.originalMissing");
                        } else if (original.getStatus() != WorkOrderStatus.COMPLETED) {
                            errors.add("confirmWorkOrder.originalNotCompleted");
                        }
                    }
                }

                Booking booking = session.get(
                        Booking.class,
                        workOrder.getBookingId(),
                        LockMode.PESSIMISTIC_WRITE
                );

                if (booking == null) {
                    errors.add("confirmWorkOrder.bookingMissing");
                } else {
                    Vehicle vehicle = session.get(
                            Vehicle.class, booking.getVehicleId()
                    );

                    if (vehicle == null) {
                        errors.add("confirmWorkOrder.vehicleMissing");
                    } else if (session.get(
                            Customer.class, vehicle.getCustomerId()) == null) {
                        errors.add("confirmWorkOrder.customerMissing");
                    }
                }


                if (workOrder.getMechanicId() == 0
                        || session.get(
                        Mechanic.class, workOrder.getMechanicId()) == null) {
                    errors.add("confirmWorkOrder.mechanicMissing");
                }

                List<Integer> serviceIds = workOrder.getServiceItemIds();

                if (serviceIds == null || serviceIds.isEmpty()) {
                    errors.add("confirmWorkOrder.servicesMissing");
                } else {
                    for (Integer serviceId : serviceIds) {
                        if (serviceId == null
                                || session.get(ServiceItem.class, serviceId) == null) {
                            errors.add("confirmWorkOrder.serviceMissing");
                            break;
                        }
                    }
                }

                if (!errors.isEmpty()) {
                    transaction.rollback();
                    return errors;
                }

                workOrder.changeStatus(WorkOrderStatus.CONFIRMED);
                transaction.commit();

                return errors;

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
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
            String comments,
            Integer originalWorkOrderId
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

                if (originalWorkOrderId != null) {
                    if (savedDraft.getType() != WorkOrderTypeEnum.COMPLAINT) {
                        throw new IllegalArgumentException("confirmWorkOrder.originalWrongType");
                    }

                    if (originalWorkOrderId.intValue() == savedDraft.getId()) {
                        throw new IllegalArgumentException("confirmWorkOrder.originalSelfReference");
                    }

                    WorkOrder original = session.get(WorkOrder.class, originalWorkOrderId);

                    if (original == null) {
                        throw new IllegalArgumentException("confirmWorkOrder.originalMissing");
                    }

                    if (original.getStatus() != WorkOrderStatus.COMPLETED) {
                        throw new IllegalArgumentException("confirmWorkOrder.originalNotCompleted");
                    }
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
                savedDraft.setOriginalWorkOrderId(originalWorkOrderId);
                savedBooking.setFrozenPrice(createFrozenPrices(savedServices));

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
        draft.setOriginalWorkOrderId(originalWorkOrderId);

        booking.setDate(plannedDate);
        booking.setDurationMinutes(minutes);
        booking.setMechanicId(mechanicId == 0 ? null : mechanicId);
        booking.setServices(new ArrayList<>(savedServices));
        booking.setFrozenPrice(createFrozenPrices(savedServices));
    }
    public void saveDropInOrder(WorkOrder workOrder, Booking booking) {
        org.hibernate.Session session = HibernateUtil.getSessionFactory().openSession();
        org.hibernate.Transaction tx = null;
        try {
            tx = session.beginTransaction();

            // 1. Spara bokningen först så att MySQL genererar dess data och fordonet länkas
            session.save(booking);
            workOrder.setBookingId(booking.getId());
            session.save(workOrder);

            tx.commit();
            System.out.println("Drop-in order och skuggbokning sparades i samma transaktion via DI!");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw new RuntimeException("Kunde inte spara drop-in-order via repository: " + e.getMessage());
        } finally {
            session.close();
        }
    }

    private List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems) {
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