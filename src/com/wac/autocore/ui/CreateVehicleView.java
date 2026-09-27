package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;


public class CreateVehicleView {

    /**
     * Bygger upp formuläret för att skapa ett nytt fordon.
     * Returnerar en VBox som kan visas i contentPane i AutoCoreApp.
     */
    public static VBox build() {
        GarageSystem garageSystem = new GarageSystem();

        VehicleRepository vehicleRepository = new VehicleRepository();

        TextField regField = new TextField();
        regField.setPromptText("Enter registration number");

        TextField brandField = new TextField();
        brandField.setPromptText("Enter brand");

        TextField modelField = new TextField();
        modelField.setPromptText("Enter model");

        TextField yearField = new TextField();
        yearField.setPromptText("Enter year");

        // Ägaren väljs i en lista i stället för att skriva in ett kund-id,
        // då kan användaren inte råka skriva ett id som inte finns.
        ComboBox<Customer> ownerBox = createOwnerBox();

        Button createButton = UiKit.primaryButton("Create vehicle");
        Label statusLabel = UiKit.feedbackLabel(); // visar resultat/felmeddelande till användaren

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
                UiKit.showError(statusLabel, "Year must be a number!");
                return; // avbryt, skapa inget fordon
            }

            Customer owner = ownerBox.getValue();
            if (owner == null) {
                UiKit.showError(statusLabel, "Please choose an owner.");
                return;
            }
            int customerId = owner.getId();

            // GarageSystem returnerar null om kund-id:t inte finns,
            // istället för att kasta ett undantag - så vi måste kolla själva.
            Vehicle vehicle = garageSystem.createVehicle(registrationNumber, brand, model, year, customerId);

            if (vehicle == null) {
                UiKit.showError(statusLabel, "Could not create vehicle. Check the owner.");
                return;
            }

            try {
                // Sparar fordonet i MySQL innan bekräftelsen visas.
                vehicleRepository.save(vehicle);
            } catch (RuntimeException exception) {
                // Ångrar tillägget i minnet om databassparandet misslyckas.
                Database.getVehicles().remove(vehicle);

                UiKit.showError(statusLabel, "The vehicle could not be saved. Please try again.");
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(statusLabel, "Vehicle created: " + vehicle.getRegistrationNumber());

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
                UiKit.pageHeader("Create vehicle", null),
                UiKit.formField("Registration number", regField),
                UiKit.formRow(
                        UiKit.formField("Brand", brandField),
                        UiKit.formField("Model", modelField)),
                UiKit.formField("Year", yearField),
                UiKit.formField("Owner", ownerBox),
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
        ownerBox.setPromptText("Choose owner");

        // Utan converter visar ComboBox kundens toString(), som innehåller alla fält.
        ownerBox.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName();
            }

            @Override
            public Customer fromString(String text) {
                return null; // behövs inte, listan är inte redigerbar
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
