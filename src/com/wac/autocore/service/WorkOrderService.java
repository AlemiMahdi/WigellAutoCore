package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.IWorkOrderRepository;
import com.wac.autocore.ui.language.LanguageManager;

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
}

