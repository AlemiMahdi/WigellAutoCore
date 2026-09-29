package com.wac.autocore.ui.views;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Locale;
public class EditBookingServicesView {

    public VBox getView(){

        LanguageManager language = LanguageManager.getInstance();

        Label titleLabel = new Label();

        titleLabel.textProperty().bind(language.text("editBookingServicesView.title"));
        titleLabel.getStyleClass().add("panel-title");

        Label bookingLabel = new Label();
        bookingLabel.textProperty().bind(language.text("editBookingServicesView.booking"));

        ComboBox<Booking> bookingComboBox = new ComboBox<>(
                FXCollections.observableArrayList(Database.getBookings())
        );

        bookingComboBox.setMaxWidth(Double.MAX_VALUE);
        bookingComboBox.promptTextProperty().bind(language.text("editBookingServicesView.selectBooking"));


        // Visar bokningens ID och datum utan modellens engelska toString-text.
        bookingComboBox.setCellFactory(list -> createBookingCell());
        bookingComboBox.setButtonCell(createBookingCell());

        Label servicesLabel = new Label();
        servicesLabel.textProperty().bind(
                language.text("editBookingServices.services")
        );

        ListView<ServiceItem> serviceList = new ListView<>(
                FXCollections.observableArrayList(Database.getServiceItems())
        );
        serviceList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serviceList.setPrefHeight(250);
        serviceList.disableProperty().bind(
                bookingComboBox.valueProperty().isNull()
        );

        serviceList.setCellFactory(list -> new ListCell<ServiceItem>() {
            @Override
            protected void updateItem(ServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.getId() + " – " + item.getName());
            }
        });

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(
                language.text("editBookingServices.empty")
        );
        serviceList.setPlaceholder(emptyLabel);

        Label helpLabel = new Label();
        helpLabel.textProperty().bind(language.text("editBookingServices.help"));
        helpLabel.setWrapText(true);

        Label totalTimeLabel = new Label();
        Label totalPriceLabel = new Label();

        Runnable updateTotals = () -> {
            int totalMinutes = serviceList.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .mapToInt(ServiceItem::getEstimatedMinutes)
                    .sum();

            double totalPrice = serviceList.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .mapToDouble(ServiceItem::getPrice)
                    .sum();

            totalTimeLabel.textProperty().bind(
                    language.text("editBookingServices.totalTime")
                            .concat(" ")
                            .concat(String.valueOf(totalMinutes))
                            .concat(" min")
            );

            totalPriceLabel.textProperty().bind(
                    language.text("editBookingServices.totalPrice")
                            .concat(" ")
                            .concat(String.format(
                                    Locale.ROOT, "%.2f SEK", totalPrice
                            ))
            );
        };

        serviceList.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<ServiceItem>) change -> updateTotals.run()
        );

        // Tillfälligt: tömmer valet när bokningen byts.
        // WAC-34 ska senare användas för att läsa in bokningens tjänster.
        bookingComboBox.valueProperty().addListener(
                (observable, previous, selected) ->
                        serviceList.getSelectionModel().clearSelection()
        );

        updateTotals.run();

        Label noticeLabel = new Label();
        noticeLabel.textProperty().bind(
                language.text("editBookingServices.preview")
        );
        noticeLabel.setWrapText(true);

        Button saveButton = new Button();
        saveButton.textProperty().bind(language.text("editBookingServices.save"));

        // Aktiveras först när validering och permanent lagring är inkopplade.
        saveButton.setDisable(true);

        VBox root = new VBox(
                10,
                titleLabel,
                bookingLabel,
                bookingLabel,
                servicesLabel,
                helpLabel,
                serviceList,
                totalTimeLabel,
                totalPriceLabel,
                noticeLabel,
                saveButton
        );

        root.setPadding(new Insets(20));
        VBox.setVgrow(serviceList, Priority.ALWAYS);

        return root;
    }

    private ListCell<Booking> createBookingCell() {
        return new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.getId() + " – " + item.getDate());
            }
        };
    }
}
