package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Formulär för att skapa nya kunder
public class CreateCustomerView extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();
    
    private final CustomerService customerService = new CustomerService(new CustomerRepository());

    // Kunduppgifter
    private final TextField nameField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();

    // Visar resultat eller felmeddelande under knappen
    private final Label feedback = UiKit.feedbackLabel();

    public CreateCustomerView() {
        // Formulärkolumnen ska ligga centrerad högst upp i innehållsytan
        setAlignment(Pos.TOP_CENTER);

        nameField.promptTextProperty().bind(language.text("customer.create.namePrompt"));
        phoneField.promptTextProperty().bind(language.text("customer.create.phonePrompt"));
        emailField.promptTextProperty().bind(language.text("customer.create.emailPrompt"));

        Button saveButton = UiKit.primaryButton("Create customer");
        saveButton.textProperty().bind(language.text("customer.create.title"));
        saveButton.setOnAction(event -> saveCustomer());

        Button backBtn = UiKit.backButton(language.text("createWorkOrder.backBtn"), "create-work-order");

        // Rubriken ligger inne i formulärkolumnen, som i designen
        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader(language.text("customer.create.title"), backBtn),
                UiKit.formField(language.text("customers.name"), nameField),
                UiKit.formField(language.text("customers.phone"), phoneField),
                UiKit.formField(language.text("customers.email"), emailField),
                saveButton,
                feedback
        ));
    }

    /** Skapar kunden via GarageSystem och sparar den i databasen. */
    private void saveCustomer() {

        // Ta bort tidigare bindning innan meddelandet ändras
        feedback.textProperty().unbind();

        try {
            Customer customer = customerService.createCustomer(
                    nameField.getText(),
                    phoneField.getText(),
                    emailField.getText()
            );

            UiKit.showSuccess(
                    feedback,
                    language.text("customer.create.success").get() + "\n" + customer
            );

            // Lyckatmeddelandet uppdateras vid språkbyte
            feedback.textProperty().bind(
                    Bindings.concat(
                            language.text("customer.create.success"),
                            "\n",
                            customer.toString()
                    )
            );

            nameField.clear();
            phoneField.clear();
            emailField.clear();

        } catch (IllegalArgumentException exception) {

            String errorKey = exception.getMessage();

            UiKit.showError(feedback, language.text(errorKey).get());

            // Felmeddelandet uppdateras vid språkbyte
            feedback.textProperty().bind(language.text(errorKey));

        } catch (RuntimeException exception) {

            UiKit.showError(
                    feedback,
                    language.text("customer.create.error").get()
            );

            feedback.textProperty().bind(
                    language.text("customer.create.error")
            );

            exception.printStackTrace();
        }
    }
}
