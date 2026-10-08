package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.collections.ObservableList;
import java.util.ArrayList;

import javafx.scene.control.ListView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javafx.collections.ListChangeListener;
import java.util.Locale;


// Formulär för att boka in ett fordon: välj fordon, datum, mekaniker och skriv en beskrivning.
public class CreateBookingView {

    private final LanguageManager language = LanguageManager.getInstance();
    private final GarageSystem garageSystem = new GarageSystem();

    public VBox getView() {

        // Fordonet väljs i en lista i stället för att skriva in ID:t för hand,
        // då kan man inte skriva fel ID.
        ComboBox<Vehicle> vehicleCombo = new ComboBox<>(
                FXCollections.observableArrayList(Database.getVehicles())
        );
        vehicleCombo.promptTextProperty().bind(language.text("createBooking.vehiclePrompt"));
        vehicleCombo.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleCombo);

        DatePicker datePicker = new DatePicker();
        datePicker.promptTextProperty().bind(language.text("createBooking.datePrompt"));

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");

        TextField durationField = new TextField();
        durationField.promptTextProperty().bind(language.text("createBooking.durationPrompt"));

        ComboBox<Mechanic> mechanicCombo = new ComboBox<>(
                FXCollections.observableArrayList(Database.getMechanics())
        );
        mechanicCombo.promptTextProperty().bind(language.text("createBooking.mechanicPrompt"));
        mechanicCombo.setConverter(mechanicConverter());
        UiKit.keepPromptWhenCleared(mechanicCombo);

        ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();

        ComboBox<ServicePackage> packageCombo = new ComboBox<>(
            FXCollections.observableArrayList(Database.getServicePackages())
        );
        packageCombo.setPromptText("Select service package");
        UiKit.keepPromptWhenCleared(packageCombo);

        durationField.setEditable(false);
        durationField.setText("0");

        Label totalPriceLabel = new Label();

        Runnable updateTotals = () -> {
            int totalMinutes = selectedServices.stream().mapToInt(ServiceItem::getEstimatedMinutes).sum();

            double totalPrice = selectedServices.stream().mapToDouble(ServiceItem::getPrice).sum();

            durationField.setText(String.valueOf(totalMinutes));
            totalPriceLabel.setText(String.format(Locale.ROOT, "%.2f", totalPrice));
        };

        selectedServices.addListener((ListChangeListener<ServiceItem>) change -> updateTotals.run());
        updateTotals.run();

        ListView<ServiceItem> serviceList = new ListView<>(
                FXCollections.observableArrayList(Database.getServiceItems())
        );

        serviceList.setPrefHeight(150);
        serviceList.setCellFactory(listView -> new javafx.scene.control.ListCell<ServiceItem>() {

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

                checkBox.setSelected(
                    selectedServices.stream()
                        .anyMatch(selected -> selected.getId() == service.getId())
                );

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

        TextArea descriptionField = new TextArea();

        packageCombo.setOnAction(event -> {
            ServicePackage selectedPackage = packageCombo.getValue();
            if (selectedPackage == null) {
                return;
            }
            selectedServices.setAll(selectedPackage.getServices());
            serviceList.refresh();
        });

        descriptionField.promptTextProperty().bind(language.text("createBooking.descriptionPrompt"));
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        Label messageLabel = UiKit.feedbackLabel();

        Button createButton = UiKit.primaryButton("Create booking");
        createButton.textProperty().bind(language.text("createBooking.button"));

        BookingRepository bookingRepository = new BookingRepository();

        createButton.setOnAction(event -> {


            Vehicle vehicle = vehicleCombo.getValue();
            if (vehicle == null) {
                UiKit.showError(messageLabel, language.text("createBooking.selectVehicle").get());
                return;

            }
            int vehicleId = vehicle.getId();

            LocalDate date = datePicker.getValue();
            if (date == null) {
                UiKit.showError(messageLabel, language.text("createBooking.selectDate").get());
                return;
            }

            LocalTime startTime;
            try {
                startTime = LocalTime.parse(startTimeField.getText().trim());
            } catch (DateTimeParseException exception) {
                UiKit.showError(messageLabel, language.text("createBooking.invalidStartTime").get());
                return;
            }

            int durationMinutes;
            try {
                durationMinutes = Integer.parseInt(durationField.getText().trim());
            } catch (NumberFormatException exception) {
                UiKit.showError(messageLabel, language.text("createBooking.invalidDuration").get());
                return;
            }

            if (durationMinutes <= 0) {
                UiKit.showError(messageLabel, language.text("createBooking.durationGreaterThanZero").get());
                return;
            }

            Mechanic mechanic = mechanicCombo.getValue();
            if (mechanic == null) {
                UiKit.showError(messageLabel, language.text("createBooking.selectMechanic").get());
                return;
            }

            if (selectedServices.isEmpty()) {
                UiKit.showError(
                        messageLabel,
                        language.text("createBooking.selectService").get()
                );
                return;
            }

            boolean overlapping = bookingRepository.hasOverlappingBooking(
                    mechanic.getId(), date, startTime, durationMinutes
            );
            if (overlapping) {
                UiKit.showError(messageLabel, language.text("createBooking.overlapping").get());
                return;
            }

            String description = descriptionField.getText();

            // GarageSystem skapar bokningen och lägger den i Database-listan
            Booking booking = garageSystem.createBooking(vehicleId, date, description);

            if (booking == null) {
                UiKit.showError(messageLabel, language.text("createBooking.createError").get());
                return;
            }

            booking.setServices(new ArrayList<>(selectedServices));
            booking.setStartTime(startTime);
            booking.setDurationMinutes(durationMinutes);

            booking.setFrozenPrice(new ArrayList<>());

            for (ServiceItem service : selectedServices) {
                booking.addFrozenPrice(
                    new InvoiceLine(
                        service.getId(),
                        service.getName(),
                        service.getPrice()
                    )
                );
            }

            try {
                // Sparar bokningen i MySQL innan vi visar en bekräftelse.
                bookingRepository.save(booking, mechanic.getId());
            } catch (RuntimeException exception) {
                // Tar bort bokningen ur minnet om databassparandet misslyckades.
                Database.getBookings().remove(booking);

                UiKit.showError(messageLabel, language.text("createBooking.saveError").get());
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(messageLabel, language.text("createBooking.success").get());

            // Tömmer formuläret så att nästa bokning kan skrivas in direkt
            vehicleCombo.getSelectionModel().clearSelection();
            vehicleCombo.setValue(null);
            datePicker.setValue(null);
            startTimeField.clear();
            durationField.clear();
            descriptionField.clear();
            mechanicCombo.getSelectionModel().clearSelection();
            mechanicCombo.setValue(null);
            selectedServices.clear();
            packageCombo.getSelectionModel().clearSelection();
            packageCombo.setValue(null);
            serviceList.refresh();
        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("createBooking.title"), null),
                UiKit.formField(language.text("bookings.vehicle"), vehicleCombo),
                UiKit.formRow(
                        UiKit.formField(language.text("bookings.date"), datePicker),
                        UiKit.formField(language.text("bookings.startTime"), startTimeField)
                ),
                UiKit.formRow(
                        UiKit.formField(language.text("createBooking.durationLabel"), durationField),
                        UiKit.formField(language.text("bookings.mechanic"), mechanicCombo)
                ),
                UiKit.formField("Service package", packageCombo),
                UiKit.formField(language.text("createBooking.services"), serviceList),
                UiKit.formField(language.text("createBooking.totalPrice"), totalPriceLabel),
                UiKit.formField(language.text("bookings.description"), descriptionField),
                createButton,
                messageLabel
        );

        // Yttre VBox centrerar formulärkolumnen i innehållsytan
        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;
    }

    // Visar fordon som "ABC123 · Volvo V70" både i listan och i den valda rutan
    private StringConverter<Vehicle> vehicleConverter() {
        return new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                if (vehicle == null) {
                    return "";
                }
                return vehicle.getRegistrationNumber() + " · "
                        + vehicle.getBrand() + " " + vehicle.getModel();
            }

            @Override
            public Vehicle fromString(String text) {
                // Används inte – combo-boxen går inte att skriva i
                return null;
            }
        };
    }

    // Visar bara mekanikerns namn i stället för hela toString()
    private StringConverter<Mechanic> mechanicConverter() {
        return new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.getName();
            }

            @Override
            public Mechanic fromString(String text) {
                return null;
            }
        };
    }
}
