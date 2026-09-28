package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.wac.autocore.ui.language.LanguageManager;

//befintliga kunder
public class CustomerView extends VBox {

    public CustomerView() {
        setSpacing(15);

        LanguageManager language = LanguageManager.getInstance();


        Label title = new Label("Customers");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        //Varje rad i tabellen representerar en kund
        TableView<Customer> table = new TableView<>();

        //kolumneran kopplas till kundens uppgifter
        TableColumn<Customer, String> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.valueOf(cell.getValue().getId())));

        TableColumn<Customer, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getPhone()));

        TableColumn<Customer, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getEmail()));

        TableColumn<Customer, String> vipColumn = new TableColumn<>("VIP");
        vipColumn.setCellValueFactory(cell ->
                language.text(
                        cell.getValue().isVip() ? "common.yes" : "common.no"
                )
        );
        title.textProperty().bind(language.text("customers.title"));
        idColumn.textProperty().bind(language.text("customers.id"));
        nameColumn.textProperty().bind(language.text("customers.name"));
        phoneColumn.textProperty().bind(language.text("customers.phone"));
        emailColumn.textProperty().bind(language.text("customers.email"));
        vipColumn.textProperty().bind(language.text("customers.vip"));

        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(phoneColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(vipColumn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("customers.empty"));
        table.setPlaceholder(emptyLabel);

        //hämtar kundlistan när vyn skapas.
        table.setItems(
                FXCollections.observableArrayList(Database.getCustomers())
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(title, table);
    }
}