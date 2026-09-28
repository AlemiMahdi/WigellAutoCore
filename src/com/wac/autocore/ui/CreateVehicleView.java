package com.wac.autocore.ui;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.GarageSystem;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.wac.autocore.data.Database;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.ui.language.LanguageManager;


public class CreateVehicleView {

    /**
     * Bygger upp formuläret för att skapa ett nytt fordon.
     * Returnerar en VBox som kan visas i contentPane i AutoCoreApp.
     */
    public static VBox build() {

        LanguageManager language = LanguageManager.getInstance();
        GarageSystem garageSystem = new GarageSystem();

        VehicleRepository vehicleRepository = new VehicleRepository();

        // GridPane ger oss ett rutnät (rader/kolumner) att placera
        // labels och textfält i, som ett formulär.
        GridPane form = new GridPane();
        form.setHgap(10); // horisontellt avstånd mellan kolumner
        form.setVgap(10); // vertikalt avstånd mellan rader

        Label regLabel = new Label("Registreringsnummer:");
        TextField regField = new TextField();

        Label brandLabel = new Label("Märke:");
        TextField brandField = new TextField();

        Label modelLabel = new Label("Model:");
        TextField modelField = new TextField();

        Label yearLabel = new Label("År:");
        TextField yearField = new TextField();

        Label customerLabel = new Label("Kund-Id");
        TextField customerField = new TextField();

        // Placera varje label/fält i rätt kolumn (0/1) och rad (0-4).
        form.add(regLabel, 0, 0);
        form.add(regField, 1, 0);
        form.add(brandLabel, 0, 1);
        form.add(brandField, 1, 1);
        form.add(modelLabel, 0, 2);
        form.add(modelField, 1, 2);
        form.add(yearLabel, 0, 3);
        form.add(yearField, 1, 3);
        form.add(customerLabel, 0, 4);
        form.add(customerField, 1, 4);

        Button createButton = new Button("Create vehicle");
        Label statusLabel = new Label(); // visar resultat/felmeddelande till användaren

        statusLabel.setWrapText(true);

        regLabel.textProperty().bind(language.text("vehicle.create.registration"));
        brandLabel.textProperty().bind(language.text("vehicle.create.brand"));
        modelLabel.textProperty().bind(language.text("vehicle.create.model"));
        yearLabel.textProperty().bind(language.text("vehicle.create.year"));
        customerLabel.textProperty().bind(language.text("vehicle.create.customerId"));
        createButton.textProperty().bind(language.text("vehicle.create.save"));

        // Körs varje gång användaren klickar på "Create vehicle".
        createButton.setOnAction(actionEvent -> {
            String registrationNumber = regField.getText();
            String brand = brandField.getText();
            String model = modelField.getText();

            int year;
            int customerId;

            // Textfälten ger oss bara String, så vi måste konvertera
            // år och kund-id till int. Om användaren skrivit bokstäver
            // istället för siffror kastas NumberFormatException.
            try {
                year = Integer.parseInt(yearField.getText());
                customerId = Integer.parseInt(customerField.getText());
            } catch (NumberFormatException e) {
                statusLabel.textProperty().bind(
                        language.text("vehicle.create.invalidNumbers")
                );
                return; // avbryt, skapa inget fordon
            }

            // GarageSystem returnerar null om kund-id:t inte finns,
            // istället för att kasta ett undantag - så vi måste kolla själva.
            Vehicle vehicle = garageSystem.createVehicle(registrationNumber, brand, model, year, customerId);

            if (vehicle == null) {
                statusLabel.textProperty().bind(
                        language.text("vehicle.create.invalidCustomer")
                );
                return;
            }

            try {
                // Sparar fordonet i MySQL innan bekräftelsen visas.
                vehicleRepository.save(vehicle);
            } catch (RuntimeException exception) {
                // Ångrar tillägget i minnet om databassparandet misslyckas.
                Database.getVehicles().remove(vehicle);

                statusLabel.textProperty().bind(
                        language.text("vehicle.create.error")
                );
                exception.printStackTrace();
                return;
            }

            statusLabel.textProperty().bind(
                    language.text("vehicle.create.success")
                            .concat(" ")
                            .concat(vehicle.getRegistrationNumber())
            );

// Tömmer formuläret efter att sparandet har lyckats.
            regField.clear();
            brandField.clear();
            modelField.clear();
            yearField.clear();
            customerField.clear();
        });

        VBox root = new VBox(15, form, createButton, statusLabel);
        root.setPadding(new Insets(20));

        return root;
    }
}