package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IServiceItemRepository;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ServiceItemService {

    private final IServiceItemRepository repository;
    public ServiceItemService(IServiceItemRepository repository) {
        this.repository = repository;
    }

    // 1450.0 -> "1 450 SEK" (mellanslag som tusentalsavgränsare, som i designen)
    public String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(price) + " SEK";
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
    public void saveDuration(ServiceItem selected, Label feedbackLabel,
                                    LanguageManager language, TextField durationField, TableView<ServiceItem> table) {


        if (selected == null) {
            UiKit.showError(feedbackLabel, language.text("services.select").get());
            return;
        }

        int minutes;

        try {
            minutes = Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException exception) {
            UiKit.showError(feedbackLabel, language.text("services.invalidNumber").get());
            return;
        }

        if (minutes <= 0) {
            UiKit.showError(feedbackLabel, language.text("services.invalidDuration").get());
            return;
        }

        // Behåller det gamla värdet om sparandet misslyckas.
        int previousMinutes = selected.getEstimatedMinutes();
        selected.setEstimatedMinutes(minutes);

        try {
            repository.save(selected);
        } catch (RuntimeException exception) {
            selected.setEstimatedMinutes(previousMinutes);
            table.refresh();

            UiKit.showError(feedbackLabel, language.text("services.error").get());
            exception.printStackTrace();
            return;
        }

        table.refresh();
        UiKit.showSuccess(feedbackLabel, language.text("services.saved").get());
    }

    public void savePrice(TableView<ServiceItem> table, Label priceFeedbackLabel,
                                 LanguageManager language, TextField priceField, ServiceItem selected) {

        if (selected == null) {
            UiKit.showError(priceFeedbackLabel, language.text("services.select").get());
            return;
        }

        double price;

        try {
            price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            UiKit.showError(priceFeedbackLabel, language.text("services.invalidPrice").get());
            return;
        }

        if (price <= 0) {
            UiKit.showError(priceFeedbackLabel, language.text("services.invalidPrice").get());
            return;
        }

        // Behåller det gamla värdet om sparandet misslyckas.
        double previousPrice = selected.getPrice();
        selected.setPrice(price);

        try {
            repository.save(selected);
        } catch (RuntimeException exception) {
            selected.setPrice(previousPrice);
            table.refresh();

            UiKit.showError(priceFeedbackLabel, language.text("services.priceError").get());
            exception.printStackTrace();
            return;
        }

        table.refresh();
        UiKit.showSuccess(priceFeedbackLabel, language.text("services.priceSaved").get());
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
