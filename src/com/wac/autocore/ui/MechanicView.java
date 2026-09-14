package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

//visar systemets befintliga mekaniker i en tabell
public class MechanicView extends VBox {

    public MechanicView() {
        setSpacing(15);

        //skapar sidans rubrik
        Label title = new Label("Mechanics");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<Mechanic> table = new TableView<>(); //inehåller en rad per mekaniker

        //kopplar kolumnerna till mekanikers uppgift
        TableColumn<Mechanic, String> idColumn =
                new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.valueOf(cell.getValue().getId())));

        TableColumn<Mechanic, String> nameColumn =
                new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<Mechanic, String> phoneColumn =
                new TableColumn<>("Phone");
        phoneColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getPhone()));

        TableColumn<Mechanic, String> specializationColumn =
                new TableColumn<>("Specialization");
        specializationColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getSpecialization()));


        //visar tillgänglighet
        TableColumn<Mechanic, String> availableColumn =
                new TableColumn<>("Available");
        availableColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().isAvailable() ? "Yes" : "No"));

        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(phoneColumn);
        table.getColumns().add(specializationColumn);
        table.getColumns().add(availableColumn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No mechanics found."));

        //hämtar aktuella mekaniker när vyn öppnas
        table.setItems(
                FXCollections.observableArrayList(Database.getMechanics())
        );

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(title, table);
    }
}