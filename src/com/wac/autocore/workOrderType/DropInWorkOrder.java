package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.service.GarageSystem;

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

                int bookingId = Database.getBookings().size() +1;
        Booking automaticBooking = new Booking();
        automaticBooking.setId(bookingId);
        automaticBooking.setVehicleId(workOrderDto.getVehicleId());
        automaticBooking.setVehicleId(workOrderDto.getVehicleId());
        automaticBooking.setDescription(workOrderDto.getDescription() != null ? workOrderDto.getDescription() : "Drop-in order");
        automaticBooking.setStatus("BOOKED");

        List<InvoiceLine> frozenPrices = new ArrayList<>();

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
                }
            }
        }
        automaticBooking.setFrozenPrice(frozenPrices);
        Database.getBookings().add(automaticBooking);

        WorkOrder workOrder = getWorkOrder(workOrderDto, automaticBooking);

        return workOrder;
    }

    private WorkOrder getWorkOrder(WorkOrderDto workOrderDto, Booking automaticBooking) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setBookingId(automaticBooking.getId());
        workOrder.setServiceItemIds(workOrderDto.getServices());
        workOrder.setMechanicId(workOrderDto.getMechanicId());
        workOrder.setType(WorkOrderTypeEnum.DROP_IN);

        workOrder.setStatus("WORK_ORDER_CREATED");

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
