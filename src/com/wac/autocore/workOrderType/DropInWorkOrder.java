package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.service.GarageSystem;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DropInWorkOrder implements WorkOrderTypeInterface{

    GarageSystem garageSystem = new GarageSystem();

    @Override
    public WorkOrder createWorkOrder(WorkOrderDto workOrderDto) {
        Vehicle vehicle = Database.getVehicles().stream()
                .filter(v -> v.getId() == workOrderDto.getVehicleId())
                .findFirst()
                .orElse(null);

        if (vehicle == null) {
            System.out.println("Vehicle with ID " + workOrderDto.getVehicleId() + " does not exist.");
            return null;
        }

        Booking automaticBooking = new Booking();
        automaticBooking.setVehicleId(workOrderDto.getVehicleId());
        automaticBooking.setDescription(workOrderDto.getDescription() != null ? workOrderDto.getDescription() : "Drop-in order");
        automaticBooking.setStatus("BOOKED");
        automaticBooking.setDate(workOrderDto.getDate());
        automaticBooking.setStartTime(workOrderDto.getStartTime());
        automaticBooking.setDurationMinutes(workOrderDto.getDuration());


        List<InvoiceLine> frozenPrices = new ArrayList<>();
        int totalDuration = 0;

        if (workOrderDto.getServices() != null) {
            for (int serviceId : workOrderDto.getServices()) {
                // Hämta den enskilda tjänsten baserat på ID
                ServiceItem service = findServiceItem(serviceId);
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

        return getWorkOrder(workOrderDto, automaticBooking);
    }

    private WorkOrder getWorkOrder(WorkOrderDto workOrderDto, Booking automaticBooking) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setBookingId(automaticBooking.getId());
        workOrder.setMechanicId(workOrderDto.getMechanicId());
        workOrder.setType(WorkOrderTypeEnum.DROP_IN);
        workOrder.setStatus("CREATED");

        if (workOrderDto.getServices() != null) {
            for (int serviceId : workOrderDto.getServices()) {
                workOrder.addServiceItem(serviceId);
            }
        }
        return workOrder;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
    private ServiceItem findServiceItem(int serviceId) {
        return Database.getServiceItems().stream()
                .filter(s -> s.getId() == serviceId)
                .findFirst()
                .orElse(null);
    }
}
