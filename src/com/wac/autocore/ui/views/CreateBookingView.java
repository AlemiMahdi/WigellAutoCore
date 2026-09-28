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
import com.wac.autocore.ui.language.LanguageManager;

public class CreateBookingView {

    LanguageManager language = LanguageManager.getInstance();

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

        // Uppdaterar texterna direkt vid språkbyte.
        title.textProperty().bind(language.text("createBooking.title"));
        vehiclesLabel.textProperty().bind(language.text("createBooking.availableVehicles"));
        vehicleIdLabel.textProperty().bind(language.text("createBooking.vehicleId"));
        vehicleIdField.promptTextProperty().bind(language.text("createBooking.vehicleIdPrompt"));
        dateLabel.textProperty().bind(language.text("createBooking.date"));
        startTimeLabel.textProperty().bind(language.text("createBooking.startTime"));
        durationLabel.textProperty().bind(language.text("createBooking.duration"));
        durationField.promptTextProperty().bind(language.text("createBooking.durationPrompt"));
        mechanicLabel.textProperty().bind(language.text("createBooking.mechanic"));
        mechanicCombo.promptTextProperty().bind(language.text("createBooking.mechanicPrompt"));
        descriptionLabel.textProperty().bind(language.text("createBooking.description"));
        createButton.textProperty().bind(language.text("createBooking.button"));

        BookingRepository bookingRepository = new BookingRepository();


        createButton.setOnAction(event -> {

            try {

                int vehicleId = Integer.parseInt(
                        vehicleIdField.getText()
                );

                LocalDate date = datePicker.getValue();
                if (date == null) {
                    messageLabel.textProperty().bind(
                            language.text("createBooking.selectDate")
                    );
                    return;
                }

                LocalTime startTime;

                try {
                startTime = LocalTime.parse(
                        startTimeField.getText().trim()
                );
                } catch (DateTimeParseException exception) {
                    messageLabel.textProperty().bind(
                            language.text("createBooking.invalidStartTime")
                    );
                return;
                }


                int durationMinutes;

                try {
                durationMinutes = Integer.parseInt(
                        durationField.getText().trim()
                );
                } catch (NumberFormatException exception) {
                    messageLabel.textProperty().bind(
                            language.text("createBooking.invalidDuration")
                    );
                return;
                }

                if (durationMinutes <= 0) {
                    messageLabel.textProperty().bind(
                            language.text("createBooking.durationGreaterThanZero")
                    );
                return;
                }

                Mechanic mechanic = mechanicCombo.getValue();

                if (mechanic == null) {
                    messageLabel.textProperty().bind(
                            language.text("createBooking.selectMechanic")
                    );
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
                    messageLabel.textProperty().bind(
                            language.text("createBooking.overlapping")
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

                        messageLabel.textProperty().bind(
                                language.text("createBooking.saveError")
                        );
                        exception.printStackTrace();
                        return;
                    }

                    messageLabel.textProperty().bind(
                            language.text("createBooking.success")
                    );

                    vehicleIdField.clear();
                    datePicker.setValue(null);
                    descriptionField.clear();
                    startTimeField.clear();
                    durationField.clear();
                    mechanicCombo.getSelectionModel().clearSelection();
                    mechanicCombo.setValue(null);

                } else {

                    messageLabel.textProperty().bind(
                            language.text("createBooking.createError")
                    );
                }

            } catch (NumberFormatException e) {

                messageLabel.textProperty().bind(
                        language.text("createBooking.invalidVehicleId")
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