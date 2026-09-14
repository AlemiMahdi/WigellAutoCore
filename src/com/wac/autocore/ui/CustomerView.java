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

public class CustomerView extends VBox {

    public CustomerView() {
        setSpacing(15);

        Label title = new Label("Customers");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<Customer> table = new TableView<>();

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
                new ReadOnlyStringWrapper(
                        cell.getValue().isVip() ? "Yes" : "No"));

        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(phoneColumn);
        table.getColumns().add(emailColumn);
        table.getColumns().add(vipColumn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No customers found."));

        table.setItems(
                FXCollections.observableArrayList(Database.getCustomers())
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(title, table);
    }
}