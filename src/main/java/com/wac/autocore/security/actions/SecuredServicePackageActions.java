package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.service.ServicePackageService;

@Component 
public class SecuredServicePackageActions {
    
    private final ServicePackageService servicePackageService;

    public SecuredServicePackageActions(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    //Alla onloggade användare får visa servicepaket
    @PreAuthorize ("isAuthenticated()")
    public List<ServicePackage> findAll(){
        return List.copyOf(servicePackageService.findAll());
    }

    //Endast ADMIN får skapa nya paket
    @PreAuthorize ("hasRole('ADMIN')")
    public ServicePackage createServicePackage(String name){
        return servicePackageService.createServicePackage(name);
    }

    //Endast ADMIN får ändra tjänster i ett servicepaket.
    @PreAuthorize ("hasRole('ADMIN')")
    public void updateServices(
        ServicePackage servicePackage,
        List<ServiceItem> serviceItems
    ){
        servicePackageService.updateServices(servicePackage, serviceItems);
    }

}
