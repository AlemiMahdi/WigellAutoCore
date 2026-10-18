package com.wac.autocore.ui.views;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class ViewHelpMethod {

    /**
     * Används i ProcessPaymentView och ShowPaymentsView så att betalsätt ser likadant ut.
     */
    public static String formatPaymentType(String type) {
        return paymentTypeText(type).getValue();
    }

    public static ObservableValue<String> paymentTypeText(String type) {
        if (type == null || type.trim().isEmpty()) {
            return new ReadOnlyStringWrapper("—");
        }

        return LanguageManager.getInstance().text(
                "payment.type." + type.trim().toUpperCase(Locale.ROOT)
        );
    }

    // 1450.0 -> "1 450 SEK" (mellanslag som tusentalsavgränsare, som i designen)
    public static String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(price) + " SEK";
    }

    //Metod för att Automatiskt hämta tjänsterna och priser, tid för dem
    //Minska koden i själva vyerna
    public static void setupServiceChecklist(
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


}

