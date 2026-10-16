package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.IWorkOrderRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.binding.StringExpression;

import java.util.ArrayList;
import java.util.List;

public class WorkOrderService {
    private final IWorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderTypeManager typeManager = new WorkOrderTypeManager();

    //Får ändra BookingRepo hämtning sen
    public WorkOrderService(IWorkOrderRepository workOrderRepository,  BookingRepository bookingRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
    }

    public WorkOrder createAndSaveWorkOrder(WorkOrderTypeEnum selectedType, WorkOrderDto dto, Booking originalBooking,
                                            Mechanic mechanic, LanguageManager language) {
        WorkOrder workOrder = typeManager.createWorkOrderType(selectedType, dto, language);

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
                        // Skapa en ny skuggbokning för reklamationen (Måste ändras om MySQL sätter ID via AUTO_INCREMENT)

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

                if (!Database.getBookings().contains(autoBooking)) {
                    Database.getBookings().add(autoBooking);
                } else {
                    throw new RuntimeException("createWorkOrder.bookingNotFound");
                }
            }
        }
    }

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

        errors = new WorkOrderRepository().confirmDraft(workOrderId);

        if (errors.isEmpty()) {
            // Uppdatera minnet först när databasen har sparats.
            workOrder.changeStatus(WorkOrderStatus.CONFIRMED);
        }

        return errors;
    }

    // Skapar en arbetsorder som utkast med en automatisk bokning (WAC-63)
    public WorkOrder createDraftWorkOrder(int vehicleId, String description, WorkOrderTypeEnum type, Integer originalWorkOrderId) {
        //1. Kontroller allt först
        Vehicle vehicle = findVehicle(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("createDraft.vehicleMissing");
        }
        if (findCustomer(vehicle.getCustomerId()) == null) {
            throw new IllegalArgumentException("createDraft.customerMissing");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("createDraft.descriptionMissing");
        }

        //2. Skapa utkastet (kastar fel om typ saknas)
        int bookingId = Database.getBookings().size() + 1;
        int workOrderId = Database.getWorkOrders().size() + 1;
        WorkOrder draft = WorkOrder.createDraft(workOrderId, bookingId, type);

        if (type == WorkOrderTypeEnum.COMPLAINT && originalWorkOrderId != null) {
            draft.setOriginalWorkOrderId(originalWorkOrderId);
        }

        //3. Skapa den automatiska bokingen
        Booking booking = new Booking(bookingId, vehicleId, null, description.trim());
        booking.setStatus("WORK_ORDER_CREATED");

        //4. Lägg till båda i listorna
        Database.getBookings().add(booking);
        Database.getWorkOrders().add(draft);

        return draft;
    }

    public List<String> validateWorkOrderForConfirmation(int workOrderId) {
        List<String> errors = new ArrayList<>();
        WorkOrder workOrder = workOrderRepository.findWorkOrder(workOrderId);

        if (workOrder == null) {
            errors.add("confirmWorkOrder.orderMissing");
            return errors;
        }

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
                WorkOrder original = workOrderRepository.findWorkOrder(originalId);

                if (original == null) {
                    errors.add("confirmWorkOrder.originalMissing");
                } else if (original.getStatus() != WorkOrderStatus.COMPLETED) {
                    errors.add("confirmWorkOrder.originalNotCompleted");
                }
            }
        }

        Booking booking = findBooking(workOrder.getBookingId());

        if (booking == null) {
            errors.add("confirmWorkOrder.bookingMissing");
        } else {
            Vehicle vehicle = findVehicle(booking.getVehicleId());

            if (vehicle == null) {
                errors.add("confirmWorkOrder.vehicleMissing");
            } else if (findCustomer(vehicle.getCustomerId()) == null) {
                errors.add("confirmWorkOrder.customerMissing");
            }
        }

        if (findMechanic(workOrder.getMechanicId()) == null) {
            errors.add("confirmWorkOrder.mechanicMissing");
        }

        List<Integer> serviceIds = workOrder.getServiceItemIds();

        if (serviceIds == null || serviceIds.isEmpty()) {
            errors.add("confirmWorkOrder.servicesMissing");
        } else {
            for (Integer serviceId : serviceIds) {
                boolean exists = serviceId != null
                        && Database.getServiceItems().stream()
                        .anyMatch(service -> service.getId() == serviceId);

                if (!exists) {
                    errors.add("confirmWorkOrder.serviceMissing");
                    break;
                }
            }
        }

        return errors;
    }

    // Samma logik som tidigare: starta via GarageSystem och spara via repositories.
    private void startSelectedWorkOrder() {

        feedbackLabel.textProperty().unbind();
        feedbackLabel.setText("");

        WorkOrder workorder = selectedWorkOrder;
        if (workorder == null) {
            UiKit.showError(feedbackLabel, language.text("startWorkOrder.select").get());
            return;
        }

        List<String> errors =
                garageSystem.validateWorkOrderForConfirmation(workorder.getId());

        if (!errors.isEmpty()) {
            StringExpression message = language.text(errors.get(0));

            for (int i = 1; i < errors.size(); i++) {
                message = message.concat("\n").concat(
                        language.text(errors.get(i))
                );
            }

            feedbackLabel.setWrapText(true);
            UiKit.showError(feedbackLabel, "");
            feedbackLabel.textProperty().bind(message);
            return;
        }

        // Sparar den valda arbetsorderns status innan vi försöker starta den.
        WorkOrderStatus previousStatus = workorder.getStatus();

        garageSystem.startWorkOrder(workorder.getId());

        if (previousStatus == WorkOrderStatus.CONFIRMED
                && workorder.getStatus() == WorkOrderStatus.IN_PROGRESS) {
            // Hämtar bokningen och mekanikern som startWorkOrder har ändrat.
            Booking booking = findBooking(workorder);
            Mechanic mechanic = findMechanic(workorder);

            try {
                // Sparar arbetsorderns, bokningens och mekanikerns nya status.
                workOrderRepository.save(workorder);

                if (booking != null) {
                    bookingRepository.save(booking);
                }

                if (mechanic != null) {
                    mechanicRepository.save(mechanic);
                }

                UiKit.showSuccess(feedbackLabel,
                        language.text("startWorkOrder.success").get() + " " + workorder.getId());
            } catch (RuntimeException exception) {
                UiKit.showError(feedbackLabel,
                        language.text("startWorkOrder.saveError").get());
                exception.printStackTrace();
            }

        } else {
            UiKit.showError(feedbackLabel, language.text("startWorkOrder.cannotStart").get());
        }

        // Återställer valet och laddar om listan (startad order försvinner ur den).
        // Inget kort väljs automatiskt, så man startar inte nästa order av misstag.
        reloadList(false);
    }
}

