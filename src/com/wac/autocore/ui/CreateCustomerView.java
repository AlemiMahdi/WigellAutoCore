package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Formulär för att skapa nya kunder
public class CreateCustomerView extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();
    private final GarageSystem garageSystem = new GarageSystem();
    private final CustomerRepository customerRepository = new CustomerRepository();

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

        // Rubriken ligger inne i formulärkolumnen, som i designen
        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader(language.text("customer.create.title"), null),
                UiKit.formField(language.text("customers.name"), nameField),
                UiKit.formField(language.text("customers.phone"), phoneField),
                UiKit.formField(language.text("customers.email"), emailField),
                saveButton,
                feedback
        ));
    }

    /** Skapar kunden via GarageSystem och sparar den i databasen. */
    private void saveCustomer() {
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

            UiKit.showError(feedback, language.text("customer.create.error").get());
            exception.printStackTrace();
            return;
        }

        UiKit.showSuccess(feedback, language.text("customer.create.success").get() + "\n" + customer);

        // Tömmer fälten först när kunden har sparats.
        nameField.clear();
        phoneField.clear();
        emailField.clear();
    }
}
