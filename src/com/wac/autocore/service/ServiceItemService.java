package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IServiceItemRepository;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.List;
import java.util.Locale;

public class ServiceItemService {

    private final IServiceItemRepository repository;

    public ServiceItemService(IServiceItemRepository repository) {
        this.repository = repository;
    }

    //Metod för att Automatiskt hämta tjänsterna och priser, tid för dem
    //Minska koden i själva vyerna
    public void setupServiceChecklist(
            ListView<ServiceItem> serviceList,
            ObservableList<ServiceItem> selectedServices,
            TextField durationField,
            Label totalPriceLabel,
            List<ServiceItem> preSelectedServices
    ) {
        // 1. Skapa logiken för att uppdatera textfält och etiketter automatiskt
        Runnable updateTotals = () -> {
            int totalMinutes = selectedServices.stream()
                    .mapToInt(ServiceItem::getEstimatedMinutes).sum();

            double totalPrice = selectedServices.stream()
                    .mapToDouble(ServiceItem::getPrice).sum();

            if (durationField != null) {
                durationField.setText(String.valueOf(totalMinutes));
            }
            if (totalPriceLabel != null) {
                totalPriceLabel.setText(String.format(Locale.ROOT, "%.2f", totalPrice));
            }
        };

        // Lyssna på förändringar (om man lägger till eller tar bort tjänster)
        selectedServices.addListener((ListChangeListener<ServiceItem>) change -> updateTotals.run());

        // 2. Bygg cell-fabriken för ListView-raderna med CheckBox
        serviceList.setCellFactory(listView -> new ListCell<ServiceItem>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(ServiceItem service, boolean empty) {
                super.updateItem(service, empty);

                if (empty || service == null) {
                    setGraphic(null);
                    return;
                }

                checkBox.setText(
                        service.getName()
                                + " – " + service.getPrice() + " kr"
                                + " – " + service.getEstimatedMinutes() + " min"
                );

                // Sätt ikryssad om tjänsten finns i listan över valda tjänster
                checkBox.setSelected(
                        selectedServices.stream()
                                .anyMatch(selected -> selected.getId() == service.getId())
                );

                // Hantera när användaren klickar på CheckBoxen
                checkBox.setOnAction(event -> {
                    if (checkBox.isSelected()) {
                        boolean alreadySelected = selectedServices.stream()
                                .anyMatch(selected -> selected.getId() == service.getId());

                        if (!alreadySelected) {
                            selectedServices.add(service);
                        }
                    } else {
                        selectedServices.removeIf(
                                selected -> selected.getId() == service.getId()
                        );
                    }
                });

                setGraphic(checkBox);
            }
        });

        // 3. Förvälj de tjänster som redan fanns sparade på bokningen/ordern sedan tidigare
        if (preSelectedServices != null && !preSelectedServices.isEmpty()) {
            for (ServiceItem service : serviceList.getItems()) {
                boolean previouslySelected = preSelectedServices.stream()
                        .anyMatch(previous -> previous.getId() == service.getId());

                if (previouslySelected) {
                    boolean alreadyAdded = selectedServices.stream()
                            .anyMatch(selected -> selected.getId() == service.getId());
                    if (!alreadyAdded) {
                        selectedServices.add(service);
                    }
                }
            }
        }

        // Kör en första beräkning och uppdatera vyn
        updateTotals.run();
        serviceList.refresh();
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
