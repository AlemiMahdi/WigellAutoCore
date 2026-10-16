package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.ui.language.LanguageManager;

import java.util.ArrayList;
import java.util.List;

public class DropInWorkOrder implements WorkOrderTypeInterface {

    @Override
    public WorkOrder createWorkOrder(WorkOrderDto workOrderDto, LanguageManager language) {
        if (workOrderDto == null || workOrderDto.getVehicleId() == null) {
            throw new IllegalArgumentException("createWorkOrder.vehicleNotFound");
        }

        Booking automaticBooking = new Booking();
        automaticBooking.setId(0);
        automaticBooking.setVehicleId(workOrderDto.getVehicleId());
        automaticBooking.setDescription(workOrderDto.getDescription() != null ? workOrderDto.getDescription() : "Drop-in order");
        automaticBooking.setStatus("WORK_ORDER_CREATED");
        automaticBooking.setDate(workOrderDto.getDate());
        automaticBooking.setStartTime(workOrderDto.getStartTime());
        automaticBooking.setDurationMinutes(workOrderDto.getDuration());
        automaticBooking.setMechanicId(workOrderDto.getMechanicId());


        List<InvoiceLine> frozenPrices = new ArrayList<>();
        int totalDuration = 0;

        if (workOrderDto.getServices() != null) {
            for (int serviceId : workOrderDto.getServices()) {
                // Hämta den enskilda tjänsten baserat på ID
                ServiceItem service = WorkOrderService.findServiceItem(serviceId);
                if (service != null) {
                    InvoiceLine frozenLine = new InvoiceLine(
                            service.getId(),
                            service.getName(),
                            service.getPrice()
                    );
                    frozenPrices.add(frozenLine);
                    //lägga till varaktigheten duration här
                    totalDuration += service.getEstimatedMinutes();
                }
            }
        }
        automaticBooking.setFrozenPrice(frozenPrices);
        automaticBooking.setDurationMinutes(totalDuration);

        Database.getBookings().add(automaticBooking);
        WorkOrder workOrder = new WorkOrder(
                0, // Sätt bookingId till 0 initialt, Servicen mappar om det efter Hibernate-save
                workOrderDto.getMechanicId(),
                WorkOrderTypeEnum.DROP_IN);

        if (workOrderDto.getServices() != null) {
            for (int serviceId : workOrderDto.getServices()) {
                workOrder.addServiceItem(serviceId);
            }
        }

        return workOrder;
    }
}
