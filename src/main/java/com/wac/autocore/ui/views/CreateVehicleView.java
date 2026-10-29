package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateVehicleView {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CreateVehicleView.class);

    /**
     * Bygger upp formuläret för att skapa ett nytt fordon.
     * Returnerar en VBox som kan visas i contentPane i AutoCoreApp.
     */
    public static VBox build(VehicleService vehicleService) {
        LanguageManager language = LanguageManager.getInstance();

        TextField regField = new TextField();
        regField.promptTextProperty().bind(language.text("vehicle.create.regPrompt"));

        TextField brandField = new TextField();
        brandField.promptTextProperty().bind(language.text("vehicle.create.brandPrompt"));

        TextField modelField = new TextField();
        modelField.promptTextProperty().bind(language.text("vehicle.create.modelPrompt"));

        TextField yearField = new TextField();
        yearField.promptTextProperty().bind(language.text("vehicle.create.yearPrompt"));

        // Ägaren väljs i en lista i stället för att skriva in ett kund-id,
        // då kan användaren inte råka skriva ett id som inte finns.
        ComboBox<Customer> ownerBox = createOwnerBox();

        Button createButton = UiKit.primaryButton("Create vehicle");
        createButton.textProperty().bind(language.text("vehicle.create.save"));
        Label statusLabel = UiKit.feedbackLabel(); // visar resultat/felmeddelande till användaren

        Button backBtn = UiKit.backButton(language.text("createWorkOrder.backBtn"), "create-work-order");

        // Körs varje gång användaren klickar på "Create vehicle".
        createButton.setOnAction(actionEvent -> {
            String registrationNumber = regField.getText();
            String brand = brandField.getText();
            String model = modelField.getText();

            int year;

            // Textfältet ger oss bara String, så vi måste konvertera
            // året till int. Om användaren skrivit bokstäver
            // istället för siffror kastas NumberFormatException.
            try {
                year = Integer.parseInt(yearField.getText().trim());
            } catch (NumberFormatException e) {
                UiKit.showError(statusLabel, language.text("vehicle.create.invalidYear").get());
                return; // avbryt, skapa inget fordon
            }

            Customer owner = ownerBox.getValue();
            if (owner == null) {
                UiKit.showError(statusLabel, language.text("vehicle.create.selectOwner").get());
                return;
            }
            int customerId = owner.getId();

            Vehicle vehicle;
            try {
                vehicle = vehicleService.createVehicle(registrationNumber, brand, model, year, customerId);
            } catch (IllegalArgumentException exception) {
                UiKit.showError(statusLabel, language.text(exception.getMessage()).get());
                return;
            } catch (RuntimeException exception) {
                UiKit.showError(statusLabel, language.text("vehicle.create.error").get());
                LOGGER.error("Could not create vehicle", exception);
                return;
            }

            UiKit.showSuccess(statusLabel, language.text("vehicle.create.success").get() + " " + vehicle.getRegistrationNumber());

            // Tömmer formuläret efter att sparandet har lyckats.
            regField.clear();
            brandField.clear();
            modelField.clear();
            yearField.clear();
            ownerBox.getSelectionModel().clearSelection();
        });

        // Rubriken ligger inne i formulärkolumnen, som i designen.
        // Märke och modell hör ihop och står därför bredvid varandra.
        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("vehicle.create.save"), backBtn),
                UiKit.formField(language.text("vehicles.registration"), regField),
                UiKit.formRow(
                        UiKit.formField(language.text("vehicles.brand"), brandField),
                        UiKit.formField(language.text("vehicles.model"), modelField)),
                UiKit.formField(language.text("vehicles.year"), yearField),
                UiKit.formField(language.text("vehicles.owner"), ownerBox),
                createButton,
                statusLabel
        );

        // Yttre VBox som centrerar formulärkolumnen högst upp i innehållsytan
        VBox root = new VBox(form);
        root.setAlignment(Pos.TOP_CENTER);
        return root;
    }

    /** Skapar listan med alla kunder. Kunden visas med sitt namn i listan. */
    private static ComboBox<Customer> createOwnerBox() {
        ComboBox<Customer> ownerBox =
                new ComboBox<>(FXCollections.observableArrayList(Database.getCustomers()));
        ownerBox.promptTextProperty().bind(LanguageManager.getInstance().text("vehicle.create.ownerPrompt"));

        // Utan converter visar ComboBox kundens toString(), som innehåller alla fält.
        ownerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String text) {
                // Måste finnas eftersom StringConverter kräver den,
                // men anropas aldrig eftersom listan inte är redigerbar.
                return null;
            }
        });

        UiKit.keepPromptWhenCleared(ownerBox);

        // Förvald första kund, som i designen, så att formuläret går snabbt att fylla i
        if (!ownerBox.getItems().isEmpty()) {
            ownerBox.getSelectionModel().selectFirst();
        }
        return ownerBox;
    }
}
