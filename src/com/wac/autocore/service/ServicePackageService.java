package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.IServicePackageRepository;

import java.util.ArrayList;
import java.util.List;

public class ServicePackageService {

    private final IServicePackageRepository servicePackageRepository;

    public ServicePackageService(IServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    public ServicePackage createServicePackage(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("createServicePackage.nameRequired");
        }
        ServicePackage servicePackage = new ServicePackage(name.trim());
        servicePackageRepository.save(servicePackage);
        // Tillfällig, annan kod i CreateBookingView läser listan
        Database.getServicePackages().add(servicePackage);

        return servicePackage;
    }

    // Läser listan i minnet, eftersom CreateBookingView delar samma objekt.
    // Byts mot repository när listorna rivs.
    public List<ServicePackage> findAll() {
        return Database.getServicePackages();
    }

    public void updateServices(ServicePackage servicePackage, List<ServiceItem> serviceItems) {


        if (servicePackage == null) {
            throw new IllegalArgumentException("manageServicePackage.selectPackage");
        }

        List<ServiceItem> previousServices =
                new ArrayList<>(servicePackage.getServices());

        servicePackage.setServices(serviceItems);

        try {
            servicePackageRepository.save(servicePackage);

        } catch (RuntimeException exception) {
            servicePackage.setServices(previousServices);
            throw exception;

        }

    }
}
