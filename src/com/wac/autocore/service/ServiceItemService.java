package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IServiceItemRepository;

public class ServiceItemService {

    private final IServiceItemRepository repository;

    public ServiceItemService(IServiceItemRepository repository) {
        this.repository = repository;
    }

    // Samma validering och sparande som tidigare, via ServiceItemRepository.
    public void saveDuration(ServiceItem selected, int minutes) {

        if (selected == null) {
            throw new IllegalArgumentException("services.select");
        }

        if (minutes <= 0) {
            throw new IllegalArgumentException("services.invalidDuration");
        }

        // Behåller det gamla värdet om sparandet misslyckas.
        int previousMinutes = selected.getEstimatedMinutes();
        selected.setEstimatedMinutes(minutes);

        try {
            repository.save(selected);
        } catch (RuntimeException exception) {
            selected.setEstimatedMinutes(previousMinutes);

            throw new RuntimeException("Kunde inte spara tid för tjänst " + selected.getId(), exception);
        }
    }

    public void savePrice(ServiceItem selected, double price) {

        if (selected == null) {
            throw new IllegalArgumentException("services.select");
        }

        if (price <= 0) {
            throw new IllegalArgumentException("services.invalidPrice");
        }

        // Behåller det gamla värdet om sparandet misslyckas.
        double previousPrice = selected.getPrice();
        selected.setPrice(price);

        try {
            repository.save(selected);
        } catch (RuntimeException exception) {
            selected.setPrice(previousPrice);
            throw new RuntimeException("Kunde inte spara pris för tjänst " + selected.getId(), exception);
        }
    }

    //Från GarageSystem för LoadSampleData, används inte just nu, men ska finnas kvar
//    public void showServiceItems() {
//        System.out.println();
//        System.out.println("=== SERVICES ===");
//
//        if (Database.getServiceItems().isEmpty()) {
//            System.out.println("No services found.");
//            return;
//        }
//
//        for (ServiceItem serviceItem : Database.getServiceItems()) {
//            System.out.println(serviceItem);
//        }
//    }
}
