package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;


public class ShowVehicleView {

    /**
     * Bygger upp sidan som visar alla fordon i systemet: rubrik, knapp och tabell.
     * Returnerar en VBox som kan visas i contentPane i AutoCoreApp.
     */
    public static VBox build() {
        LanguageManager language = LanguageManager.getInstance();

        // Knappen tar användaren direkt till formuläret, precis som ett klick i menyn
        Button newVehicleButton = UiKit.primaryButton("+ New vehicle");
        newVehicleButton.textProperty().bind(language.text("vehicles.new"));
        newVehicleButton.setOnAction(event -> Navigator.goTo("create-vehicle"));

        TableView<Vehicle> table = createVehicleTable();

        // Hämtar alla fordon från "databasen" och gör om listan till en
        // ObservableList, som TableView kräver för att kunna visa datan.
        ObservableList<Vehicle> data = FXCollections.observableArrayList(Database.getVehicles());
        table.setItems(data);

        VBox root = new VBox(20, UiKit.pageHeader(language.text("vehicles.title"), newVehicleButton), table);
        return root;
    }

    /** Bygger tabellen med kolumnerna Reg. no, Brand & model, Year och Owner. */
    private static TableView<Vehicle> createVehicleTable() {
        TableView<Vehicle> table = new TableView<>();
        LanguageManager language = LanguageManager.getInstance();

        // Varje kolumn får sitt värde via en lambda som läser från Vehicle.
        // (Lambdan ger kompileringsfel om en getter stavas fel, till skillnad
        // från PropertyValueFactory som bara tyst visar en tom cell.)
        TableColumn<Vehicle, String> regCol = new TableColumn<>("Reg. no");
        regCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getRegistrationNumber()));

        // Märke och modell visas ihop, t.ex. "Volvo V70"
        TableColumn<Vehicle, String> brandModelCol = new TableColumn<>("Brand & model");
        brandModelCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getBrand() + " " + cell.getValue().getModel()));

        TableColumn<Vehicle, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getYear())));

        // Fordonet sparar bara kundens id - vi slår upp namnet så att det blir läsbart
        TableColumn<Vehicle, String> ownerCol = new TableColumn<>("Owner");
        ownerCol.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(findOwnerName(cell.getValue().getCustomerId())));

        regCol.textProperty().bind(language.text("vehicles.registration"));
        brandModelCol.textProperty().bind(language.text("vehicles.brandModel"));
        yearCol.textProperty().bind(language.text("vehicles.year"));
        ownerCol.textProperty().bind(language.text("vehicles.owner"));

        // Registreringsnumret är radens "nyckel" och visas i fetstil, år och ägare dämpat
        regCol.getStyleClass().add("cell-strong");
        yearCol.getStyleClass().add("cell-muted");
        ownerCol.getStyleClass().add("cell-muted");

        // Lägg till kolumnerna i tabellen, i den ordning de ska visas.
        table.getColumns().add(regCol);
        table.getColumns().add(brandModelCol);
        table.getColumns().add(yearCol);
        table.getColumns().add(ownerCol);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("vehicles.empty"));
        table.setPlaceholder(emptyLabel);

        // Gemensam tabellstil: ramar, radhöjd och höjd som följer antalet rader
        UiKit.styleTable(table);
        return table;
    }

    /** Letar upp kundens namn i kundlistan. Visar id:t om kunden inte finns. */
    private static String findOwnerName(int customerId) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == customerId) {
                return customer.getName();
            }
        }
        return LanguageManager.getInstance().text("vehicles.unknownCustomer").get() + customerId;
    }
}
