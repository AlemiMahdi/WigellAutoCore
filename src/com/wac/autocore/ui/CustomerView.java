package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

// Visar alla befintliga kunder i en tabell
public class CustomerView extends VBox {

    public CustomerView() {
        setSpacing(20);

        // Knappen tar användaren direkt till formuläret, precis som ett klick i menyn
        Button newCustomerButton = UiKit.primaryButton("+ New customer");
        newCustomerButton.setOnAction(event -> Navigator.goTo("create-customer"));

        TableView<Customer> table = createCustomerTable();

        // Hämtar kundlistan när vyn skapas
        table.setItems(FXCollections.observableArrayList(Database.getCustomers()));

        getChildren().addAll(UiKit.pageHeader("Customers", newCustomerButton), table);
    }

    /** Bygger tabellen med kolumnerna ID, Name, Phone, Email och VIP. */
    private TableView<Customer> createCustomerTable() {
        // Varje rad i tabellen representerar en kund
        TableView<Customer> table = new TableView<>();

        TableColumn<Customer, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getId())));

        TableColumn<Customer, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getPhone()));

        TableColumn<Customer, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getEmail()));

        // VIP-kunder får texten "VIP" (visas som gul badge), övriga "—" (visas som dämpat streck)
        TableColumn<Customer, String> vipColumn = new TableColumn<>("VIP");
        vipColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().isVip() ? "VIP" : "—"));
        vipColumn.setCellFactory(UiKit.<Customer>badgeCells());

        // Telefon och e-post är sekundär information och visas därför dämpat
        phoneColumn.getStyleClass().add("cell-muted");
        emailColumn.getStyleClass().add("cell-muted");

        // Breddförhållanden: med CONSTRAINED_RESIZE_POLICY delas bredden ut i proportion
        // till kolumnernas maxWidth. ID och VIP blir smala, e-post får mest plats.
        idColumn.setMaxWidth(1000);
        nameColumn.setMaxWidth(3000);
        phoneColumn.setMaxWidth(3000);
        emailColumn.setMaxWidth(5000);
        vipColumn.setMaxWidth(1500);
        // Minsta bredd för e-post så att adressen inte klipps till "…".
        emailColumn.setMinWidth(230);

        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(phoneColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(vipColumn);

        table.setPlaceholder(new Label("No customers found."));

        // Gemensam tabellstil: ramar, radhöjd och höjd som följer antalet rader
        UiKit.styleTable(table);
        return table;
    }
}
