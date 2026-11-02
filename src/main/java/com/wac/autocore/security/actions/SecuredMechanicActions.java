package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.service.MechanicService;

@Component 
public class SecuredMechanicActions {
    
    private final MechanicService mechanicService;

    public SecuredMechanicActions(MechanicService mechanicService) {
        this.mechanicService = mechanicService;
    }

    //Alla inloggade användare får se mekaniker
    @PreAuthorize("isAuthenticated()")
    public List<Mechanic> findAll(){
        return mechanicService.findAll();
    }

    //Hämtar mekaniker till JavaFX
    @PreAuthorize("isAuthenticated()")
    public  List<Mechanic> getLoadedMechanics(){
        return mechanicService.getLoadedMechanics();
    }

    //Alla inloggade användare får se mekanikers schema
    @PreAuthorize("isAuthenticated()")
    public List<Booking> getSchedule(int mechanicId) {
        return mechanicService.getSchedule(mechanicId);
    }

    @PreAuthorize ("hasRole('ADMIN')")
    public void save(Mechanic mechanic) {
        mechanicService.save(mechanic);
    }


}
