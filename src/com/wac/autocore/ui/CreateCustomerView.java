package com.wac.autocore.ui;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.GarageSystem;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateCustomerView extends VBox {

    public CreateCustomerView() {
        setSpacing(10);

        Label title = new Label("Create customer");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        Button saveButton = new Button("Save customer");
        Label confirmation = new Label();
        confirmation.setWrapText(true);

        GarageSystem garageSystem = new GarageSystem();

        saveButton.setOnAction(event -> {
            Customer customer = garageSystem.createCustomer(
                    nameField.getText(),
                    phoneField.getText(),
                    emailField.getText()
            );

            confirmation.setText(
                    "Customer created successfully.\n" + customer
            );

            nameField.clear();
            phoneField.clear();
            emailField.clear();
        });

        getChildren().addAll(
                title,
                new Label("Name:"),
                nameField,
                new Label("Phone:"),
                phoneField,
                new Label("Email:"),
                emailField,
                saveButton,
                confirmation
        );
    }
}