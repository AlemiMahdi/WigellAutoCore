package com.wac.autocore.workOrderType;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.DTO.WorkOrderResult;
import com.wac.autocore.model.*;
import com.wac.autocore.data.Database;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;


public class ComplaintWorkOrder implements WorkOrderTypeInterface{


    @Override
    public WorkOrder createWorkOrder(WorkOrderDto dto, LanguageManager language, Label feedbackLabel) {

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
    //Lägg in språk hantering här
        if (booking == null) {
            UiKit.showError(feedbackLabel, "");
            feedbackLabel.textProperty().bind(language.text("createWorkOrder.bookingNotFound"));
            return null;
        }

        WorkOrder order = new WorkOrder(
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

    @Override
    public void completeWorkOrder(int workOrderId) {

    }
}
