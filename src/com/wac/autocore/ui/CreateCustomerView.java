package com.wac.autocore.ui;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.GarageSystem;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.wac.autocore.data.Database;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.ui.language.LanguageManager;

//formulär för att skapa nya kunder
public class CreateCustomerView extends VBox {

    public CreateCustomerView() {
        setSpacing(10);

        LanguageManager language = LanguageManager.getInstance();

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

        title.textProperty().bind(language.text("customer.create.title"));
        saveButton.textProperty().bind(language.text("customer.create.save"));

        Label nameLabel = new Label();
        nameLabel.textProperty().bind(language.text("customer.create.name"));

        Label phoneLabel = new Label();
        phoneLabel.textProperty().bind(language.text("customer.create.phone"));

        Label emailLabel = new Label();
        emailLabel.textProperty().bind(language.text("customer.create.email"));

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

            confirmation.textProperty().bind(
                    language.text("customer.create.success")
                            .concat("\n")
                            .concat(language.text("customers.id"))
                            .concat(": " + customer.getId() + "\n")
                            .concat(language.text("customer.create.name"))
                            .concat(" " + customer.getName() + "\n")
                            .concat(language.text("customer.create.phone"))
                            .concat(" " + customer.getPhone() + "\n")
                            .concat(language.text("customer.create.email"))
                            .concat(" " + customer.getEmail() + "\n")
                            .concat(language.text("customers.vip"))
                            .concat(": ")
                            .concat(language.text(
                                    customer.isVip() ? "common.yes" : "common.no"
                            ))
            );

            // Tömmer fälten först när kunden har sparats.
            nameField.clear();
            phoneField.clear();
            emailField.clear();
        });

        getChildren().addAll(
                title,
                nameLabel,
                nameField,
                phoneLabel,
                phoneField,
                emailLabel,
                emailField,
                saveButton,
                confirmation
        );
    }
}