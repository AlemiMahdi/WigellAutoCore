package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.VehicleService;

@Component 
public class SecuredVehicleActions {
    
    private final VehicleService vehicleService;

    public SecuredVehicleActions( VehicleService vehicleService){
        this.vehicleService = vehicleService;
    }

    @PreAuthorize ("isAuthenticated()")
    public List<Vehicle> findAll(){
        return vehicleService.findAll();
    }

    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public Vehicle createVehicle(
        String registerNumber,
        String brand,
        String model,
        int year,
        int customerId
    ){
        return  vehicleService.createVehicle(registerNumber, brand, model, year, customerId);
    }
}
