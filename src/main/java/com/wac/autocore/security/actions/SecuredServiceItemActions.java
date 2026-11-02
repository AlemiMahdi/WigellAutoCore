package com.wac.autocore.security.actions;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.service.ServiceItemService;

@Component 
public class SecuredServiceItemActions {
    
    private final ServiceItemService serviceItemService;

    public  SecuredServiceItemActions(ServiceItemService serviceItemService) {
        this.serviceItemService = serviceItemService;
    }

    //ADMIN och WORKSHOP får ändra uppskattad arbetstid
    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public void saveDuration(ServiceItem selected, int minutes) {
        serviceItemService.saveDuration(selected, minutes);
    }

    //Endast admin får ändra tjänsten pris
    @PreAuthorize ("hasRole('ADMIN')")
    public void savePrice(ServiceItem selected,double price) {
        serviceItemService.savePrice(selected, price);
    }



}
