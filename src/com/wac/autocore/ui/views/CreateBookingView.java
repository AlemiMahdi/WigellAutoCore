package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.service.GarageSystem;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.wac.autocore.model.Mechanic;

public class CreateBookingView {

    private final GarageSystem garageSystem = new GarageSystem();

    public VBox getView() {

        Label title = new Label("CREATE BOOKING");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        Label vehiclesLabel = new Label("Available vehicles:");

        ListView<Vehicle> vehicleList = new ListView<>(
                FXCollections.observableArrayList(
                        Database.getVehicles()
                )
        );

        vehicleList.setPrefHeight(150);


        Label vehicleIdLabel = new Label("Vehicle ID:");

        TextField vehicleIdField = new TextField();

        vehicleIdField.setPromptText("Enter vehicle ID");


        Label dateLabel = new Label("Date: ");

        DatePicker datePicker = new DatePicker();

        Label startTimeLabel = new Label("Start time:");

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");


        Label durationLabel = new Label("Duration (minutes):");

        TextField durationField = new TextField();
        durationField.setPromptText("Example: 60");

        Label mechanicLabel = new Label("Mechanic:");

        ComboBox<Mechanic> mechanicCombo = new ComboBox<>(
                FXCollections.observableArrayList(Database.getMechanics())
        );

        mechanicCombo.setPromptText("Select mechanic");
        mechanicCombo.setMaxWidth(Double.MAX_VALUE);

        Label descriptionLabel = new Label("Description:");

        TextArea descriptionField = new TextArea();

        descriptionField.setPrefRowCount(4);

        Label messageLabel = new Label();

        Button createButton = new Button("Create booking");

        BookingRepository bookingRepository = new BookingRepository();


        createButton.setOnAction(event -> {

            try {

                int vehicleId = Integer.parseInt(
                        vehicleIdField.getText()
                );

                LocalDate date = datePicker.getValue();
                if (date == null) {
                    messageLabel.setText("Please select a date.");
                    return;
                }

                LocalTime startTime;

                try {
                startTime = LocalTime.parse(
                        startTimeField.getText().trim()
                );
                } catch (DateTimeParseException exception) {
                messageLabel.setText(
                        "Please enter start time as HH:mm."
                );
                return;
                }


                int durationMinutes;

                try {
                durationMinutes = Integer.parseInt(
                        durationField.getText().trim()
                );
                } catch (NumberFormatException exception) {
                messageLabel.setText(
                        "Please enter a valid duration."
                );
                return;
                }

                if (durationMinutes <= 0) {
                messageLabel.setText(
                        "Duration must be greater than 0."
                );
                return;
                }

                Mechanic mechanic = mechanicCombo.getValue();

                if (mechanic == null) {
                    messageLabel.setText("Please select a mechanic.");
                    return;
                }

                boolean overlapping =
                        bookingRepository.hasOverlappingBooking(
                                mechanic.getId(),
                                date,
                                startTime,
                                durationMinutes
                        );

                if (overlapping) {      
                messageLabel.setText(
                        "The mechanic already has a booking during this time."
                );
                return;
                }

                String description =
                        descriptionField.getText();


                Booking booking =
                        garageSystem.createBooking(
                                vehicleId,
                                date,
                                description
                        );
                
                if (booking != null) {
                booking.setStartTime(startTime);
                booking.setDurationMinutes(durationMinutes);

                    try {
                        // Sparar bokningen i MySQL innan vi visar en bekräftelse.
                        bookingRepository.save(booking, mechanic.getId());
                    } catch (RuntimeException exception) {
                        // Tar bort bokningen ur minnet om databassparandet misslyckades.
                        Database.getBookings().remove(booking);

                        messageLabel.setText(
                                "Booking could not be saved. Please try again."
                        );
                        exception.printStackTrace();
                        return;
                    }

                    messageLabel.setText(
                            "Booking created successfully."
                    );

                    vehicleIdField.clear();
                    datePicker.setValue(null);
                    descriptionField.clear();
                    startTimeField.clear();
                    durationField.clear();
                    mechanicCombo.getSelectionModel().clearSelection();
                    mechanicCombo.setValue(null);

                } else {

                    messageLabel.setText(
                            "Booking could not be created."
                    );
                }

            } catch (NumberFormatException e) {

                messageLabel.setText(
                        "Please enter a valid vehicle ID."
                );

            }
        });


        VBox view = new VBox(10);

        view.setPadding(new Insets(10));

        view.getChildren().addAll(
                title,
                vehiclesLabel,
                vehicleList,
                vehicleIdLabel,
                vehicleIdField,
                dateLabel,
                datePicker,
                startTimeLabel,
                startTimeField,
                durationLabel,
                durationField,
                mechanicLabel,
                mechanicCombo,
                descriptionLabel,
                descriptionField,
                createButton,
                messageLabel
        );

        return view;
    }
}