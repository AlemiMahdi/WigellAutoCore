package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Vehicle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;


public class ShowVehicleView {

    /**
     * Bygger upp en tabell som visar alla fordon i systemet.
     * Returnerar en TableView som kan visas i contentPane i AutoCoreApp.
     */
    public static TableView<Vehicle> build() {
        TableView<Vehicle> table = new TableView<>();

        // Varje TableColumn kopplas till en kolumnrubrik (visas för användaren)
        // och ett property-namn (måste matcha en get-metod i Vehicle, t.ex.
        // "id" -> getId()). PropertyValueFactory använder reflection för att
        // hämta värdet, så stavningen måste stämma exakt med getter-namnet.
        TableColumn<Vehicle, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Vehicle, String> regCol = new TableColumn<>("Reg. nr");
        regCol.setCellValueFactory(new PropertyValueFactory<>("registrationNumber"));

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Märke");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, Integer> yearCol = new TableColumn<>("År");
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));

        TableColumn<Vehicle, Integer> customerCol = new TableColumn<>("Kund-Id");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerId"));

        // Lägg till kolumnerna i tabellen, i den ordning de ska visas.
        table.getColumns().addAll(idCol, regCol, brandCol, modelCol, yearCol, customerCol);

        // Hämtar alla fordon från "databasen" och gör om listan till en
        // ObservableList, som TableView kräver för att kunna visa datan.
        ObservableList<Vehicle> data = FXCollections.observableArrayList(Database.getVehicles());
        table.setItems(data);

        return table;
    }
}