package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.data.Database;

public class ComplaintWorkOrder implements WorkOrderTypeInterface{


    @Override
    public WorkOrder createWorkOrder(WorkOrderDto dto) {

        Booking booking = Database.getBookings().stream()
                .filter(b -> b.getId() == dto.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            System.out.println("Booking not found: " + dto.getBookingId());
            return null;
        }

        WorkOrder order = new WorkOrder(
                0,
                dto.getBookingId(),
                dto.getMechanicId(),
                WorkOrderTypeEnum.COMPLAINT
        );

        for (ServiceItem serviceItem : booking.getServices()) {
            order.addServiceItem(serviceItem.getId());
        }

        return order;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
