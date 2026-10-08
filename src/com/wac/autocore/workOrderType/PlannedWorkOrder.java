package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;

public class PlannedWorkOrder implements WorkOrderTypeInterface{

    @Override
    public WorkOrder createWorkOrder(WorkOrderDto dto) {

        Booking booking = Database.getBookings().stream()
                .filter(b -> b.getId() == dto.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            System.out.println("Booking with ID " + dto.getBookingId() + " does not exist.");
            return null;
        }
        //Skapa variabel och sätt in type enum
        WorkOrder order = new WorkOrder();
        order.setBookingId(dto.getBookingId());
        order.setMechanicId(dto.getMechanicId());
        order.setType(WorkOrderTypeEnum.PLANNED);
        order.setStatus("WORK_ORDER_CREATED");

        for (ServiceItem serviceItem : booking.getServices()) {
            order.addServiceItem(serviceItem.getId());
        }
        return order;
    }

    //ifall de behövs, kan ta bort de sen annars, om man vill ha det på olika sätt
    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
