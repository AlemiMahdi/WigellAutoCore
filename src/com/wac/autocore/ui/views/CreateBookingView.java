package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.UiKit;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;

// Formulär för att boka in ett fordon: välj fordon, datum, mekaniker och skriv en beskrivning.
public class CreateBookingView {

    private final GarageSystem garageSystem = new GarageSystem();

    public VBox getView() {

        // Fordonet väljs i en lista i stället för att skriva in ID:t för hand,
        // då kan man inte skriva fel ID.
        ComboBox<Vehicle> vehicleCombo = new ComboBox<>(
                FXCollections.observableArrayList(Database.getVehicles())
        );
        vehicleCombo.setPromptText("Select vehicle");
        vehicleCombo.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleCombo);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select date");

        ComboBox<Mechanic> mechanicCombo = new ComboBox<>(
                FXCollections.observableArrayList(Database.getMechanics())
        );
        mechanicCombo.setPromptText("Select mechanic");
        mechanicCombo.setConverter(mechanicConverter());
        UiKit.keepPromptWhenCleared(mechanicCombo);

        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("What should be done?");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        Label messageLabel = UiKit.feedbackLabel();

        Button createButton = UiKit.primaryButton("Create booking");

        BookingRepository bookingRepository = new BookingRepository();

        createButton.setOnAction(event -> {

            Vehicle vehicle = vehicleCombo.getValue();
            if (vehicle == null) {
                UiKit.showError(messageLabel, "Please select a vehicle.");
                return;
            }
            int vehicleId = vehicle.getId();

            LocalDate date = datePicker.getValue();
            if (date == null) {
                UiKit.showError(messageLabel, "Please select a date.");
                return;
            }

            Mechanic mechanic = mechanicCombo.getValue();
            if (mechanic == null) {
                UiKit.showError(messageLabel, "Please select a mechanic.");
                return;
            }

            String description = descriptionField.getText();

            // GarageSystem skapar bokningen och lägger den i Database-listan
            Booking booking = garageSystem.createBooking(vehicleId, date, description);

            if (booking == null) {
                UiKit.showError(messageLabel, "Booking could not be created.");
                return;
            }

            try {
                // Sparar bokningen i MySQL innan vi visar en bekräftelse.
                bookingRepository.save(booking, mechanic.getId());
            } catch (RuntimeException exception) {
                // Tar bort bokningen ur minnet om databassparandet misslyckades.
                Database.getBookings().remove(booking);

                UiKit.showError(messageLabel, "Booking could not be saved. Please try again.");
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(messageLabel, "Booking created successfully.");

            // Tömmer formuläret så att nästa bokning kan skrivas in direkt
            vehicleCombo.getSelectionModel().clearSelection();
            vehicleCombo.setValue(null);
            datePicker.setValue(null);
            descriptionField.clear();
            mechanicCombo.getSelectionModel().clearSelection();
            mechanicCombo.setValue(null);
        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader("Create booking", null),
                UiKit.formField("Vehicle", vehicleCombo),
                UiKit.formRow(
                        UiKit.formField("Date", datePicker),
                        UiKit.formField("Mechanic", mechanicCombo)
                ),
                UiKit.formField("Description", descriptionField),
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
