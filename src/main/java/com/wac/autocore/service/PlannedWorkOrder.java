package com.wac.autocore.service;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderTypeEnum;

public class PlannedWorkOrder implements WorkOrderTypeInterface {

    @Override
    public WorkOrder createWorkOrder(WorkOrderDto dto) {

        Booking booking = Database.getBookings().stream()
                .filter(b -> b.getId() == dto.getBookingId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("createWorkOrder.bookingNotFound"));

        //Skapa variabel och sätt in type enum
        WorkOrder order = new WorkOrder(
                0,
                dto.getMechanicId(),
                WorkOrderTypeEnum.PLANNED
        );

        order.setBookingId(booking.getId());
        for (ServiceItem serviceItem : booking.getServices()) {
            order.addServiceItem(serviceItem.getId());
        }
        return order;
    }
}
