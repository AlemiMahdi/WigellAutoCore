package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkOrderService.class);
    private final IWorkOrderRepository workOrderRepository;
    private final IBookingRepository bookingRepository;
    private final IServiceItemRepository serviceItemRepository;
    private final WorkOrderTypeManager typeManager;
    private final IVehicleRepository vehicleRepository;
    private final IMechanicRepository mechanicRepository;


    public WorkOrderService(IWorkOrderRepository workOrderRepository,  IBookingRepository bookingRepository,
                            IServiceItemRepository serviceItemRepository, IVehicleRepository vehicleRepository, IMechanicRepository mechanicRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.vehicleRepository = vehicleRepository;
        this.typeManager = new WorkOrderTypeManager(serviceItemRepository);
        this.mechanicRepository = mechanicRepository;
    }

    public void createAndSaveWorkOrder(WorkOrderTypeEnum selectedType, WorkOrderDto dto, Booking originalBooking) {
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

                        Booking complaintBooking = new Booking(
                                originalBooking.getVehicleId(),
                                null,
                                originalBooking.getDescription()
                        );
                        complaintBooking.setStatus("WORK_ORDER_CREATED");
                        complaintBooking.setMechanicId(dto.getMechanicId());
                        createFrozenComplaintPrice(workOrder, complaintBooking);

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
                        bookingRepository.update(originalBooking);
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
        LOGGER.info("Workorder {} created, type {}", workOrder.getId(), selectedType);
    }

    public void createFrozenComplaintPrice(WorkOrder workOrder, Booking complaintBooking){
        // Räkna ut tid och frys priser för reklamationen
        List<InvoiceLine> frozenLines = new ArrayList<>();
        int totalMinutes = 0;
        List<Integer> serviceItems = workOrder.getServiceItemIds();
        List<ServiceItem> services = serviceItemRepository.findAllByIds(serviceItems);
        if (services.size() != workOrder.getServiceItemIds().size()) {
            throw new IllegalArgumentException("editWorkOrderDraft.serviceMissing");
        }

        for (ServiceItem service : services) {
            totalMinutes += service.getEstimatedMinutes();
            frozenLines.add(new InvoiceLine(service.getId(), service.getName(), 0.0));
        }

        complaintBooking.setFrozenPrice(frozenLines);
        complaintBooking.setDurationMinutes(totalMinutes);
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
    public ServiceItem findServiceItem(int serviceId) {
        return serviceItemRepository.findById(serviceId);
    }

    public List<String> confirmWorkOrder(int workOrderId) {
        WorkOrder workOrder = workOrderRepository.findWorkOrder(workOrderId);
        List<String> errors = new ArrayList<>();

        if (workOrder == null) {
            errors.add("confirmWorkOrder.orderMissing");
            return errors;
        }

        if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.CONFIRMED)) {
            errors.add("confirmWorkOrder.onlyDraft");
            return errors;
        }

        errors = workOrderRepository.confirmDraft(workOrderId);

        if (errors.isEmpty()) {
            // Uppdatera minnet först när databasen har sparats.
            workOrder.changeStatus(WorkOrderStatus.CONFIRMED);
            WorkOrder inMemory = findWorkOrderInMemory(workOrderId);
            if (inMemory != null) {
                inMemory.changeStatus(WorkOrderStatus.CONFIRMED);
            }
            LOGGER.info("Workorder {} has been confirmed.", workOrderId);

        }

        return errors;
    }

    // Slutför en påbörjad arbetsorder: sparar i databasen och uppdaterar sedan minnet.
    public void completeWorkOrder(int workOrderId) {
        // Kontrollera mot databasen (den som avgör vad som faktiskt är sparat)
        WorkOrder workOrder = workOrderRepository.findWorkOrder(workOrderId);
        if (workOrder == null) {
            throw new IllegalArgumentException("completeWorkOrder.notFound");
        }
        if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.COMPLETED)) {
            throw new IllegalArgumentException("completeWorkOrder.invalidStatus");
        }

        // Spara i databasen (order, mekaniker och bokning i en transaktion)
        workOrderRepository.completeWorkOrder(workOrderId);

        LOGGER.info("Workorder {} has been completed.", workOrderId);

        // Uppdatera minnet först när databasen har sparats,
        // annars ligger ordern kvar i listan tills appen startas om.
        WorkOrder inMemory = findWorkOrderInMemory(workOrderId);
        if (inMemory != null) {
            inMemory.changeStatus(WorkOrderStatus.COMPLETED);

            Mechanic mechanic = findMechanicInMemory(inMemory.getMechanicId());

            if (mechanic != null) {
                boolean hasActiveOrders = Database.getWorkOrders().stream()
                        .anyMatch(order ->
                                order.getId() != workOrderId
                                && order.getMechanicId() == mechanic.getId()
                                && order.getStatus() == WorkOrderStatus.IN_PROGRESS
                        );

                mechanic.setAvailable(!hasActiveOrders);
            }

            Booking booking = findBookingInMemory(inMemory.getBookingId());
            if (booking != null && !sharesOriginalBooking(inMemory)) {
                booking.setStatus("COMPLETED");
            }
        }
    }

    // Skapar en arbetsorder som utkast med en automatisk bokning (WAC-63)
    public void updateDraftWorkOrder(WorkOrder draft,
                                          Booking booking,
                                          List<Integer> serviceIds,
                                          int mechanicId,
                                          LocalDate plannedDate,
                                          int minutes,
                                          BigDecimal price,
                                          String instructions,
                                          String comments,
                                          Integer originalWorkOrderId) {
        if (draft == null || !draft.getStatus().isEditable()) {
            throw new IllegalArgumentException("editWorkOrderDraft.onlyDraft");
        }

        if (booking == null || booking.getId() != draft.getBookingId()) {
            throw new IllegalArgumentException("editWorkOrderDraft.bookingMissing");
        }

        if (minutes < 0) {
            throw new IllegalArgumentException("editWorkOrderDraft.invalidMinutes");
        }
        BigDecimal normalizedPrice = getBigDecimalPrice(price);

        String finalInstructions = instructions == null ? "" : instructions.trim();
        String finalComments = comments == null ? "" : comments.trim();

        if (finalInstructions.length() > 4000 || finalComments.length() > 4000) {
            throw new IllegalArgumentException("editWorkOrderDraft.textTooLong");
        }

        List<Integer> selectedIds = new ArrayList<>(new LinkedHashSet<>(serviceIds));

        workOrderRepository.updateDraft(
                draft.getId(), booking.getId(), selectedIds, mechanicId, normalizedPrice,
                finalInstructions, finalComments, originalWorkOrderId, plannedDate, minutes
        );
        draft.setServiceItemIds(new ArrayList<>(selectedIds));
        draft.setMechanicId(mechanicId);
        draft.setEstimatedPrice(normalizedPrice);
        draft.setCustomerInstructions(finalInstructions);
        draft.setDraftComments(finalComments);
        draft.setOriginalWorkOrderId(originalWorkOrderId);

        //Uppdatera minnet till ShowBookingView
        Booking targetBooking = Database.getBookings().stream()
                .filter(b -> b.getId() == booking.getId())
                .findFirst()
                .orElse(booking);

        targetBooking.setDate(plannedDate);
        targetBooking.setDurationMinutes(minutes);
        targetBooking.setMechanicId(mechanicId == 0 ? null : mechanicId);


        List<ServiceItem> updatedServices = new ArrayList<>();
        for (Integer serviceItem : selectedIds) {
            updatedServices.add(serviceItemRepository.findById(serviceItem));
        }
        targetBooking.setServices(updatedServices);
        targetBooking.setFrozenPrice(workOrderRepository.createFrozenPrices(updatedServices));

        if(targetBooking != booking) {
            booking.setDate(plannedDate);
            booking.setDurationMinutes(minutes);
            booking.setMechanicId(mechanicId);
            booking.setServices(updatedServices);
            booking.setFrozenPrice(targetBooking.getFrozenPrice());
        }
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

        Booking booking = new Booking(vehicleId, null, description.trim());
        booking.setStatus("WORK_ORDER_CREATED");

        // 3. Spara bokning och utkast i samma transaktion
        workOrderRepository.saveWithBooking(draft, booking);

        // 4. Lägg till i minnet, först när det är sparat (annars syns utkastet inte förrän omstart)
        Database.getBookings().add(booking);
        Database.getWorkOrders().add(draft);

        LOGGER.info("Draft {} created for vehicle {}.", draft.getId(), vehicleId);

        return draft;
    }

    // Hämtar arbetsordrar som enligt statusreglerna får startas (flyttad från StartWorkOrderView, WAC-97).
    public List<WorkOrder> findStartableWorkOrders() {

        List<WorkOrder> startable = new ArrayList<WorkOrder>();
        // Läser från Database så att vyn får samma objekt som resten av appen använder.
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            // Statusmodellen avgör (idag bara CONFIRMED → IN_PROGRESS).
            if (workOrder.getStatus().canChangeTo(WorkOrderStatus.IN_PROGRESS)) {
                startable.add(workOrder);
            }
        }
        return startable;
    }

    // Startar en bekräftad arbetsorder (flyttad från GarageSystem/StartWorkOrderView, WAC-97).
    public void startWorkOrder(int workOrderId) {
        // 1. Hitta ordern
        WorkOrder workOrder = findWorkOrderInMemory(workOrderId);
        if (workOrder == null) {
            throw new IllegalArgumentException("confirmWorkOrder.orderMissing");
        }

        // 2. Får den startas?
        if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.IN_PROGRESS)) {
            throw new IllegalArgumentException("startWorkOrder.cannotStart");
        }

        // 3. Övriga kontroller (bokning, fordon, kund, mekaniker, tjänster)
        validateBeforeStart(workOrder);

        // 4. Är mekanikern ledig? (finns, det kontrollerades i steg 3)
        Mechanic mechanic = findMechanicInMemory(workOrder.getMechanicId());
        if (!mechanic.isAvailable()) {
            throw new IllegalArgumentException("startWorkOrder.mechanicBusy");
        }
        Booking booking = findBookingInMemory(workOrder.getBookingId());

        // 5. Spara undan gamla värden ifall databasen inte går att nå
        String previousBookingStatus = booking.getStatus();

        // 6. Ändra i minnet
        // Reklamationer ska inte ändra originalbokningens status.
        if (!sharesOriginalBooking(workOrder)) {
            booking.setStatus("IN_PROGRESS");
        }
        mechanic.setAvailable(false);
        workOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);

        // 7. Uppdatera istället i databasen
        try {
            workOrderRepository.update(workOrder);
            bookingRepository.update(booking);
            mechanicRepository.update(mechanic);
        } catch (RuntimeException exception) {
            // Återställ det som går att återställa i minnet
            booking.setStatus(previousBookingStatus);
            mechanic.setAvailable(true);
            throw new RuntimeException("startWorkOrder.saveError", exception);
        }
        LOGGER.info("Starting workOrder {}.", workOrderId);
    }

    // Samma kontroller som vid bekräftelse (från GarageSystem.validateWorkOrderForConfirmation).
    // Första felet kastas som språknyckel.
    private void validateBeforeStart(WorkOrder workOrder) {
        // a. Typ måste finnas
        if (workOrder.getType() == null) {
            throw new IllegalArgumentException("confirmWorkOrder.typeMissing");
        }

        // b. Reklamation: originalet måste finnas, inte vara sig själv och vara slutfört
        if (workOrder.getType() == WorkOrderTypeEnum.COMPLAINT) {
            Integer originalId = workOrder.getOriginalWorkOrderId();
            if (originalId == null) {
                throw new IllegalArgumentException("confirmWorkOrder.originalRequired");
            }
            if (originalId == workOrder.getId()) {
                throw new IllegalArgumentException("confirmWorkOrder.originalSelfReference");
            }
            WorkOrder original = findWorkOrderInMemory(originalId);
            if (original == null) {
                throw new IllegalArgumentException("confirmWorkOrder.originalMissing");
            }
            if (original.getStatus() != WorkOrderStatus.COMPLETED) {
                throw new IllegalArgumentException("confirmWorkOrder.originalNotCompleted");
            }
        }

        // c. Bokning → fordon → kund
        Booking booking = findBookingInMemory(workOrder.getBookingId());
        if (booking == null) {
            throw new IllegalArgumentException("confirmWorkOrder.bookingMissing");
        }

        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("confirmWorkOrder.vehicleMissing");
        }

        boolean customerExists = Database.getCustomers().stream()
                .anyMatch(customer -> customer.getId() == vehicle.getCustomerId());
        if (!customerExists) {
            throw new IllegalArgumentException("confirmWorkOrder.customerMissing");
        }

        // d. Mekanikern måste finnas
        if (findMechanicInMemory(workOrder.getMechanicId()) == null) {
            throw new IllegalArgumentException("confirmWorkOrder.mechanicMissing");
        }

        // e. Minst en tjänst, och alla måste finnas
        List<Integer> serviceIds = workOrder.getServiceItemIds();
        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new IllegalArgumentException("confirmWorkOrder.servicesMissing");
        }
        for (Integer serviceId : serviceIds) {
            boolean exists = serviceId != null && Database.getServiceItems().stream()
                    .anyMatch(service -> service.getId() == serviceId);
            if (!exists) {
                throw new IllegalArgumentException("confirmWorkOrder.serviceMissing");
            }
        }
    }

    private WorkOrder findWorkOrderInMemory(int id) {
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            if (workOrder.getId() == id) {
                return workOrder;
            }
        }
        return null;
    }

    private Booking findBookingInMemory(int id) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == id) {
                return booking;
            }
        }
        return null;
    }

    private Mechanic findMechanicInMemory(int id) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == id) {
                return mechanic;
            }
        }
        return null;
    }

    // En reklamation som delar bokning med sin originalorder ska
    // inte ändra bokningens status.
    private boolean sharesOriginalBooking(WorkOrder workOrder) {
        if (workOrder.getType() != WorkOrderTypeEnum.COMPLAINT
            || workOrder.getOriginalWorkOrderId() == null) {
            return false;
        }
        WorkOrder original = findWorkOrderInMemory(workOrder.getOriginalWorkOrderId());
        return original != null && original.getBookingId() == workOrder.getBookingId();
    }

    public List<WorkOrder> findAll() { return workOrderRepository.findAll(); }
}

