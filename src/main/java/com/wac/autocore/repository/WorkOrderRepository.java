package com.wac.autocore.repository;

import com.wac.autocore.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional
public class WorkOrderRepository implements IWorkOrderRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public WorkOrder save(WorkOrder workOrder) {
        entityManager.persist(workOrder);
        return workOrder;
    }

    // Sparar bokningen och utkastet i samma transaktion (WAC-63)
    // Antingen sparas båda, eller ingen av dem.
    public void saveWithBooking(WorkOrder workOrder, Booking booking) {
        entityManager.persist(booking);
        workOrder.setBookingId(booking.getId());
        entityManager.persist(workOrder);
    }

    public WorkOrder findWorkOrder(int id) {
        return entityManager.find(WorkOrder.class, id);
    }

    public List<WorkOrder> findAll() {
        return entityManager.createQuery(
                "from WorkOrder order by id",
                WorkOrder.class
        ).getResultList();
    }

    public List<String> confirmDraft(int id) {
        List<String> errors = new ArrayList<>();
        WorkOrder workOrder = entityManager.find(
                WorkOrder.class, id,
                LockModeType.PESSIMISTIC_WRITE
        );

        validateWorkOrder(workOrder, errors);
        validateWorkOrderType(workOrder, errors);
        validateBookingCustomer(workOrder, errors);
        validateMechanic(workOrder, errors);
        validateServiceItems(workOrder, errors);

        if (!errors.isEmpty()) {
            return errors;
        }

        workOrder.changeStatus(WorkOrderStatus.CONFIRMED);
        return errors;
    }

    public void updateDraft(
            int draft,
            int bookingId,
            List<Integer> selectedIds,
            int mechanicId,
            BigDecimal normalizedPrice,
            String savedInstructions,
            String savedComments,
            Integer originalWorkOrderId,
            LocalDate plannedDate,
            int minutes
    ) {
        WorkOrder savedDraft = entityManager.find(
                WorkOrder.class, draft, LockModeType.PESSIMISTIC_WRITE
        );

        validateSavedDraft(savedDraft);
        validateOriginalId(originalWorkOrderId, savedDraft);

        WorkOrder original = entityManager.find(WorkOrder.class, originalWorkOrderId);
        validateOriginal(original);

        Booking savedBooking = entityManager.find(
                Booking.class,
                savedDraft.getBookingId(),
                LockModeType.PESSIMISTIC_WRITE
        );
        validateBooking(savedBooking, bookingId);
        validateUpdateDraftMechanic(mechanicId);
        List<ServiceItem> savedServices = validateUpdateDraftServices(selectedIds);

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
    }

    public void saveDropInOrder(WorkOrder workOrder, Booking booking) {
        entityManager.persist(booking);
        workOrder.setBookingId(booking.getId());
        entityManager.persist(workOrder);
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

    public void completeWorkOrder(int workOrderId) {
                WorkOrder workOrder = entityManager.find(WorkOrder.class, workOrderId);
                Mechanic mechanic = entityManager.find(Mechanic.class, workOrder.getMechanicId());
                Booking booking = entityManager.find(Booking.class, workOrder.getBookingId());

                workOrder.changeStatus(WorkOrderStatus.COMPLETED);

                if (mechanic != null) {
                    mechanic.setAvailable(true);
                    entityManager.merge(mechanic);
                }

                if (booking != null && !sharesOriginalBooking(booking.getId(), workOrderId)) {
                    booking.setStatus("COMPLETED");
                    entityManager.merge(booking);
                }
                entityManager.merge(workOrder);
    }

    // Returnerar true om någon annan order delar på denna bokning
    private boolean sharesOriginalBooking(int workOrderId, int bookingId) {
        if (bookingId == 0) return false;
        Long count = entityManager.createQuery(
                "select count(w) from WorkOrder w where w.bookingId = :bId and w.id != :currentId", Long.class)
                .setParameter("bId", bookingId)
                .setParameter("currentId", workOrderId)
                .getSingleResult();
                return count > 0;
    }

    //Hjälpmetoder ConfirmDraft först
    private void validateWorkOrder(WorkOrder workOrder, List<String> errors) {
        if (workOrder == null) {
            errors.add("confirmWorkOrder.orderMissing");
        } else if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.CONFIRMED)) {
            errors.add("confirmWorkOrder.onlyDraft");
        }
    }

    private void validateWorkOrderType(WorkOrder workOrder, List<String> errors) {
        if (workOrder.getType() == null) {
            errors.add("confirmWorkOrder.typeMissing");
        }

        if (workOrder.getType() == WorkOrderTypeEnum.COMPLAINT) {
            Integer originalId = workOrder.getOriginalWorkOrderId();

            if (originalId == null) {
                errors.add("confirmWorkOrder.originalRequired");
            } else if (originalId == workOrder.getId()) {
                errors.add("confirmWorkOrder.originalSelfReference");
            } else {
                WorkOrder original = entityManager.find(WorkOrder.class, originalId);

                if (original == null) {
                    errors.add("confirmWorkOrder.originalMissing");
                } else if (original.getStatus() != WorkOrderStatus.COMPLETED) {
                    errors.add("confirmWorkOrder.originalNotCompleted");
                }
            }
        }
    }

    private void validateBookingCustomer(WorkOrder workOrder, List<String> errors) {
        Booking booking = entityManager.find(
                Booking.class,
                workOrder.getBookingId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (booking == null) {
            errors.add("confirmWorkOrder.bookingMissing");
        } else {
            Vehicle vehicle = entityManager.find(
                    Vehicle.class, booking.getVehicleId()
            );

            if (vehicle == null) {
                errors.add("confirmWorkOrder.vehicleMissing");
            } else if (entityManager.find(
                    Customer.class, vehicle.getCustomerId()) == null) {
                errors.add("confirmWorkOrder.customerMissing");
            }
        }
    }

    private void validateMechanic(WorkOrder workOrder, List<String> errors) {
        if (workOrder.getMechanicId() == 0
                || entityManager.find(
                Mechanic.class, workOrder.getMechanicId()) == null) {
            errors.add("confirmWorkOrder.mechanicMissing");
        }
    }

    private void validateServiceItems(WorkOrder workOrder, List<String> errors) {
        List<Integer> serviceIds = workOrder.getServiceItemIds();

        if (serviceIds == null || serviceIds.isEmpty()) {
            errors.add("confirmWorkOrder.servicesMissing");
        } else {
            for (Integer serviceId : serviceIds) {
                if (serviceId == null
                        || entityManager.find(ServiceItem.class, serviceId) == null) {
                    errors.add("confirmWorkOrder.serviceMissing");
                    break;
                }
            }
        }
    }

    //Hjälpmetoder UpdateDraft
    private void validateSavedDraft(WorkOrder savedDraft) {
        if (savedDraft == null
                || !savedDraft.getStatus().isEditable()) {
            throw new IllegalArgumentException(
                    "editWorkOrderDraft.onlyDraft"
            );
        }
    }

    private void validateOriginalId(Integer originalWorkOrderId, WorkOrder savedDraft) {
        if (originalWorkOrderId != null) {
            if (savedDraft.getType() != WorkOrderTypeEnum.COMPLAINT) {
                throw new IllegalArgumentException("confirmWorkOrder.originalWrongType");
            }

            if (originalWorkOrderId == savedDraft.getId()) {
                throw new IllegalArgumentException("confirmWorkOrder.originalSelfReference");
            }
        }
    }

    private void validateOriginal(WorkOrder original) {
        if (original == null) {
            throw new IllegalArgumentException("confirmWorkOrder.originalMissing");
        }

        if (original.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new IllegalArgumentException("confirmWorkOrder.originalNotCompleted");
        }
    }

    private void validateBooking(Booking savedBooking, int bookingId) {
        if (savedBooking == null || savedBooking.getId() != bookingId) {
            throw new IllegalArgumentException(
                    "editWorkOrderDraft.bookingMissing"
            );
        }
    }

    private void validateUpdateDraftMechanic(int mechanicId) {
        if (mechanicId != 0
                && entityManager.find(Mechanic.class, mechanicId) == null) {
            throw new IllegalArgumentException(
                    "editWorkOrderDraft.mechanicMissing"
            );
        }
    }

    private List<ServiceItem> validateUpdateDraftServices(List<Integer> selectedIds) {
        List<ServiceItem> savedServices = new ArrayList<>();
        for (Integer serviceId : selectedIds) {
            if (serviceId != null) {
                ServiceItem service = entityManager.find(ServiceItem.class, serviceId);
                if (service == null) {
                    throw new IllegalArgumentException("editWorkOrderDraft.serviceMissing");
                }
                savedServices.add(service);
            }
        } return savedServices;
    }
}
