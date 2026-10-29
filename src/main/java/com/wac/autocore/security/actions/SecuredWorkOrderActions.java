package com.wac.autocore.security.actions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.service.WorkOrderService;

@Component 
public class SecuredWorkOrderActions {
    
    private final WorkOrderService workOrderService;

    public SecuredWorkOrderActions(WorkOrderService workOrderService){
        this.workOrderService = workOrderService;
    }

    //Alla tre roller får läsa arbetsorder
    @PreAuthorize ("isAuthenticated()")
    public List<WorkOrder> findAll(){
        return workOrderService.findAll();
    }

    //Endast admin och verKstadspersonal får starta
    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void startWorkOrder(int workOrderId){
        workOrderService.startWorkOrder(workOrderId);
    }

    //Endast admin och verkstadspersonal får avsluta
    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void completedWorkOrder(int workOrderId){
        workOrderService.completeWorkOrder(workOrderId);
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public List<String> confirmWorkOrder(int workOrderId){
        return workOrderService.confirmWorkOrder(workOrderId);
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public WorkOrder createNewDraftWorkOrder(
        int vehicleId,
        String description,
        WorkOrderTypeEnum type,
        Integer originalWorkOrderId
    ){
        return workOrderService.createNewDraftWorkOrder(
            vehicleId, 
            description, 
            type, 
            originalWorkOrderId
        );
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void createAndSaveWorkOrder(
        WorkOrderTypeEnum selectedType,
        WorkOrderDto dto,
        Booking originalBooking
    ){
        workOrderService.createAndSaveWorkOrder(
            selectedType, 
            dto, 
            originalBooking);
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void updateDraftWorkOrder(
        WorkOrder draft,
        Booking booking,
        List<Integer> serviceIds,
        int mechanicId,
        LocalDate plannedDate,
        int minutes,
        BigDecimal price,
        String instructions,
        String comments,
        Integer originalWorkOrderId
    ){
        workOrderService.updateDraftWorkOrder(
            draft, 
            booking, 
            serviceIds, 
            mechanicId, 
            plannedDate, 
            minutes, 
            price, 
            instructions, 
            comments, 
            originalWorkOrderId);
    }


    @PreAuthorize("isAuthenticated()")
    public List<WorkOrder> findStartableWorkOrders(){
        return workOrderService.findStartableWorkOrders();
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void completeWorkOrder(int workOrderId) {
        workOrderService.completeWorkOrder(workOrderId);
    }
}
