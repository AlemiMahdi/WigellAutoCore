package com.wac.autocore.ui;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.GarageSystem;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.wac.autocore.data.Database;
import com.wac.autocore.repository.CustomerRepository;

//formulär för att skapa nya kunder
public class CreateCustomerView extends VBox {

    public CreateCustomerView() {
        setSpacing(10);

        Label title = new Label("Create customer");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");


        //kunduppgifter
        TextField nameField = new TextField();
        TextField phoneField = new TextField();
        TextField emailField = new TextField();

        Button saveButton = new Button("Save customer");
        Label confirmation = new Label();
        confirmation.setWrapText(true);

        GarageSystem garageSystem = new GarageSystem();

        CustomerRepository customerRepository = new CustomerRepository();

        saveButton.setOnAction(event -> {
            Customer customer = garageSystem.createCustomer(
                    nameField.getText(),
                    phoneField.getText(),
                    emailField.getText()
            );

            try {
                // Sparar kunden i MySQL innan vi visar en bekräftelse.
                customerRepository.save(customer);
            } catch (RuntimeException exception) {
                // Tar bort kunden ur minnet om databassparandet misslyckas.
                Database.getCustomers().remove(customer);

                confirmation.setText(
                        "Customer could not be saved. Please try again."
                );
                exception.printStackTrace();
                return;
            }

            confirmation.setText(
                    "Customer created successfully.\n" + customer
            );

            // Tömmer fälten först när kunden har sparats.
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