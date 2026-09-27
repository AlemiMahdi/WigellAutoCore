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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;


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

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");

        TextField durationField = new TextField();
        durationField.setPromptText("Example: 60");

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

            LocalTime startTime;
            try {
                startTime = LocalTime.parse(startTimeField.getText().trim());
            } catch (DateTimeParseException exception) {
                UiKit.showError(messageLabel, "Please enter start time as HH:mm.");
                return;
            }

            int durationMinutes;
            try {
                durationMinutes = Integer.parseInt(durationField.getText().trim());
            } catch (NumberFormatException exception) {
                UiKit.showError(messageLabel, "Please enter a valid duration.");
                return;
            }

            if (durationMinutes <= 0) {
                UiKit.showError(messageLabel, "Duration must be greater than 0.");
                return;
            }

            Mechanic mechanic = mechanicCombo.getValue();
            if (mechanic == null) {
                UiKit.showError(messageLabel, "Please select a mechanic.");
                return;
            }

            boolean overlapping = bookingRepository.hasOverlappingBooking(
                    mechanic.getId(), date, startTime, durationMinutes
            );
            if (overlapping) {
                UiKit.showError(messageLabel, "The mechanic already has a booking during this time.");
                return;
            }

            String description = descriptionField.getText();

            // GarageSystem skapar bokningen och lägger den i Database-listan
            Booking booking = garageSystem.createBooking(vehicleId, date, description);

            if (booking == null) {
                UiKit.showError(messageLabel, "Booking could not be created.");
                return;
            }
            booking.setStartTime(startTime);
            booking.setDurationMinutes(durationMinutes);

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
            startTimeField.clear();
            durationField.clear();
            descriptionField.clear();
            mechanicCombo.getSelectionModel().clearSelection();
            mechanicCombo.setValue(null);
        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader("Create booking", null),
                UiKit.formField("Vehicle", vehicleCombo),
                UiKit.formRow(
                        UiKit.formField("Date", datePicker),
                        UiKit.formField("Start time", startTimeField)
                ),
                UiKit.formRow(
                        UiKit.formField("Duration (minutes)", durationField),
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
