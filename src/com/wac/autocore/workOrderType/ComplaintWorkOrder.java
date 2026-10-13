package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.data.Database;


public class ComplaintWorkOrder implements WorkOrderTypeInterface{


    @Override
    public WorkOrder createWorkOrder(WorkOrderDto dto) {

        Integer originalId = dto.getOriginalWorkOrderId();

        if (originalId == null) {
            return null;
        }

        WorkOrder original = Database.getWorkOrders().stream()
                .filter(existing -> existing.getId() == originalId.intValue())
                .findFirst()
                .orElse(null);

        if (original == null
                || original.getStatus() != WorkOrderStatus.COMPLETED
                || original.getServiceItemIds() == null
                || original.getServiceItemIds().isEmpty()) {
            return null;
        }

        Booking booking = Database.getBookings().stream()
                .filter(b -> b.getId() == original.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            System.out.println("Booking not found: " + original.getBookingId());
            return null;
        }

        WorkOrder order = new WorkOrder(
                0,
                booking.getId(),
                dto.getMechanicId(),
                WorkOrderTypeEnum.COMPLAINT
        );

        for (Integer serviceId : original.getServiceItemIds()) {
            order.addServiceItem(serviceId);
        }

        return order;
    }

    @Override
    public void startWorkOrder(int workOrderId) {

    }
}
