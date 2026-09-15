package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import javafx.scene.control.DatePicker;

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

        Label descriptionLabel = new Label("Description:");

        TextArea descriptionField = new TextArea();

        descriptionField.setPrefRowCount(4);

        Label messageLabel = new Label();

        Button createButton = new Button("Create booking");


        createButton.setOnAction(event -> {

            try {

                int vehicleId = Integer.parseInt(
                        vehicleIdField.getText()
                );

                LocalDate date = datePicker.getValue();

                String description =
                        descriptionField.getText();


                Booking booking =
                        garageSystem.createBooking(
                                vehicleId,
                                date,
                                description
                        );


                if (booking != null) {

                    messageLabel.setText(
                            "Booking created successfully."
                    );

                    vehicleIdField.clear();
                    datePicker.setValue(null);
                    descriptionField.clear();

                } else {

                    messageLabel.setText(
                            "Booking could not be created."
                    );
                }

            } catch (NumberFormatException e) {

                messageLabel.setText(
                        "Please enter a valid vehicle ID."
                );

            } if (datePicker.getValue() == null) {
                messageLabel.setText("Please select a date.");
                return;
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
                descriptionLabel,
                descriptionField,
                createButton,
                messageLabel
        );

        return view;
    }
}