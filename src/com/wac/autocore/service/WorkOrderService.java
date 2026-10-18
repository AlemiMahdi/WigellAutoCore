package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.IServiceItemRepository;
import com.wac.autocore.repository.IVehicleRepository;
import com.wac.autocore.repository.IWorkOrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class WorkOrderService {
    private final IWorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderTypeManager typeManager = new WorkOrderTypeManager();
    private final IServiceItemRepository serviceItemRepository;
    private final IVehicleRepository vehicleRepository;

    //Får ändra BookingRepo hämtning sen
    public WorkOrderService(IWorkOrderRepository workOrderRepository,  BookingRepository bookingRepository, IServiceItemRepository serviceItemRepository, IVehicleRepository vehicleRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public WorkOrder createAndSaveWorkOrder(WorkOrderTypeEnum selectedType, WorkOrderDto dto, Booking originalBooking) {
        WorkOrder workOrder = typeManager.createWorkOrderType(selectedType, dto);

        if (workOrder == null) {
            throw new IllegalStateException("createWorkOrder.createError");
        }

        // Spara undan ursprunglig status ifall databasen kraschar
        String previousBookingStatus = (originalBooking != null) ? originalBooking.getStatus() : null;

        try {
            // 3. Switch-sats för att hantera sparningen i databasen per typ
            switch (selectedType) {
                case COMPLAINT:
                    if (originalBooking != null) {
                        // Skapa en ny skuggbokning för reklamationen (id = 0 MySQL ger id via AUTO_INCREMENT)

                        Booking complaintBooking = new Booking(0,
                                originalBooking.getVehicleId(),
                                null,
                                originalBooking.getDescription()
                        );
                        complaintBooking.setStatus("WORK_ORDER_CREATED");
                        complaintBooking.setMechanicId(dto.getMechanicId());

                        // Räkna ut tid och frys priser för reklamationen
                        List<InvoiceLine> frozenLines = new ArrayList<>();
                        int totalMinutes = 0;
                        for (Integer serviceId : workOrder.getServiceItemIds()) {
                            ServiceItem service = Database.getServiceItems().stream()
                                    .filter(item -> item.getId() == serviceId).findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException("editWorkOrderDraft.serviceMissing"));

                            totalMinutes += service.getEstimatedMinutes();
                            frozenLines.add(new InvoiceLine(service.getId(), service.getName(), 0.0));
                        }
                        complaintBooking.setFrozenPrice(frozenLines);
                        complaintBooking.setDurationMinutes(totalMinutes);

                        // Spara via Hibernate
                        workOrderRepository.saveWithBooking(workOrder, complaintBooking);

                        // Synka minnet
                        workOrder.setBookingId(complaintBooking.getId());
                        Database.getBookings().add(complaintBooking);
                    }
                    break;

                case DROP_IN:
                    // Drop-In strategin har redan lagt till sin bokning sist i listan, hämta den
                    saveAutoBooking(workOrder);
                    break;

                case PLANNED:
                    if (originalBooking != null) {
                        originalBooking.setStatus("WORK_ORDER_CREATED");
                        bookingRepository.save(originalBooking);
                    }
                    workOrderRepository.save(workOrder);
                    break;
            }

            // Gemensam synk för alla typer efter lyckad sparning
            if (!Database.getWorkOrders().contains(workOrder)) {
                Database.getWorkOrders().add(workOrder);
            }

        } catch (RuntimeException exception) {
            // Gemensam återställning i minnet vid fel
            Database.getWorkOrders().remove(workOrder);
            if (originalBooking != null && selectedType == WorkOrderTypeEnum.PLANNED) {
                originalBooking.setStatus(previousBookingStatus);
            } else if (selectedType == WorkOrderTypeEnum.DROP_IN) {
                int autoBookingId = workOrder.getBookingId();
                Database.getBookings().removeIf(b -> b.getId() == autoBookingId);
            }
            throw new RuntimeException("createWorkOrder.saveError", exception);
        }

        return workOrder;
    }

    private void saveAutoBooking(WorkOrder workOrder) {
        if (!Database.getBookings().isEmpty()) {
            Booking autoBooking = Database.getBookings().get(Database.getBookings().size() - 1);

            if (autoBooking != null && autoBooking.getId() == workOrder.getBookingId()) {
                workOrderRepository.saveDropInOrder(workOrder, autoBooking);
            }
        }
    }

    //byt till repository sökning
    public static ServiceItem findServiceItem(int serviceId) {
        return Database.getServiceItems().stream()
                .filter(s -> s.getId() == serviceId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("createWorkOrder.empty"));
    }

    public List<String> confirmWorkOrder(int workOrderId) {
        WorkOrder workOrder = workOrderRepository.findWorkOrder(workOrderId);
        List<String> errors = new ArrayList<>();

        if (workOrder == null) {
            errors.add("confirmWorkOrder.orderMissing");
            return errors;
        }

        if (workOrder.getStatus() != WorkOrderStatus.DRAFT) {
            errors.add("confirmWorkOrder.onlyDraft");
            return errors;
        }

        errors = workOrderRepository.confirmDraft(workOrderId);

        if (errors.isEmpty()) {
            // Uppdatera minnet först när databasen har sparats.
            workOrder.changeStatus(WorkOrderStatus.CONFIRMED);
        }

        return errors;
    }

    public void completeWorkOrder(int workOrderId) {

        WorkOrder workOrder = workOrderRepository.findWorkOrder(workOrderId);
        if (workOrder == null) {
            throw new IllegalArgumentException("workOrder.notFound");
        }

        if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.COMPLETED)) {
            throw new IllegalArgumentException("workOrder.cannotCompleteStatus");
        }
        workOrderRepository.completeWorkOrder(workOrderId);
    }

    // Skapar en arbetsorder som utkast med en automatisk bokning (WAC-63)
    public void updateDraftWorkOrder(WorkOrder draft,
                                          Booking booking,
                                          List<Integer> serviceIds,
                                          int mechanicId,
                                          LocalDate plannedDate,
                                          int minutes,
                                          BigDecimal price,
                                          String savedInstructions,
                                          String comments,
                                          Integer originalWorkOrderId) {
        //1. Kontroller allt först
        if (draft == null || draft.getStatus() != WorkOrderStatus.DRAFT) {
            throw new IllegalArgumentException("editWorkOrderDraft.onlyDraft");
        }

        if (booking == null || booking.getId() != draft.getBookingId()) {
            throw new IllegalArgumentException("editWorkOrderDraft.bookingMissing");
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("editWorkOrderDraft.invalidMinutes");
        }
        BigDecimal normalizedPrice = getBigDecimalPrice(price);

        String instructions = savedInstructions == null ? "" : savedInstructions.trim();
        String savedComments = comments == null ? "" : comments.trim();

        if (instructions.length() > 4000 || savedComments.length() > 4000) {
            throw new IllegalArgumentException("editWorkOrderDraft.textTooLong");
        }

        List<Integer> selectedIds = new ArrayList<>(new LinkedHashSet<>(serviceIds));

        workOrderRepository.updateDraft(
                draft.getId(), booking.getId(), selectedIds, mechanicId, normalizedPrice,
                savedInstructions, savedComments, originalWorkOrderId, plannedDate, minutes
        );
        draft.setServiceItemIds(new ArrayList<>(selectedIds));
        draft.setMechanicId(mechanicId);
        draft.setEstimatedPrice(normalizedPrice);
        draft.setCustomerInstructions(savedInstructions);
        draft.setDraftComments(savedComments);
        draft.setOriginalWorkOrderId(originalWorkOrderId);

        booking.setDate(plannedDate);
        booking.setDurationMinutes(minutes);
        booking.setMechanicId(mechanicId == 0 ? null : mechanicId);

        List<ServiceItem> updatedServices = new ArrayList<>();
        for (Integer serviceItem : selectedIds) {
            updatedServices.add(serviceItemRepository.findById(serviceItem));
        }
        booking.setServices(updatedServices);
        booking.setFrozenPrice(workOrderRepository.createFrozenPrices(updatedServices));
    }

    private BigDecimal getBigDecimalPrice(BigDecimal price) {
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
        return normalizedPrice;
    }

    public WorkOrder createNewDraftWorkOrder(int vehicleId, String description, WorkOrderTypeEnum type, Integer originalWorkOrderId) {
        // 1. Validera grunddata (fordon, beskrivning etc.)
        Vehicle vehicle = vehicleRepository.findById(vehicleId);

        if (vehicle == null) {
            throw new IllegalArgumentException("createDraft.vehicleMissing");
        }

        boolean customerExists = Database.getCustomers().stream()
                .anyMatch(customer -> customer.getId() == vehicle.getCustomerId());

        if (!customerExists) {
            throw new IllegalArgumentException("createDraft.customerMissing");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("createDraft.descriptionMissing");
        }

        // 2. Skapa utkastet och bokningen (id 0 = MySQL ger id till båda,
        //    saveWithBooking kopplar ihop dem när bokningen fått sitt id)
        WorkOrder draft = WorkOrder.createDraft(0, 0, type);
        if (type == WorkOrderTypeEnum.COMPLAINT && originalWorkOrderId != null) {
            draft.setOriginalWorkOrderId(originalWorkOrderId);
        }

        Booking booking = new Booking(0, vehicleId, null, description.trim());
        booking.setStatus("WORK_ORDER_CREATED");

        // 3. Spara bokning och utkast i samma transaktion
        workOrderRepository.saveWithBooking(draft, booking);

        // 4. Lägg till i minnet, först när det är sparat
        Database.getBookings().add(booking);
        Database.getWorkOrders().add(draft);

        // 5. Returnera utkastet
        return draft;
    }
}

