package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import com.wac.autocore.repository.ServiceItemRepository;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

//visar befintliga tjänster i en tabell
public class ServiceView extends VBox {

    public ServiceView() {
        setSpacing(15);

        //rubrik
        Label title = new Label("Services");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TableView<ServiceItem> table = new TableView<>();

        // kopplaer kollumerna till tjänstens uppgifter
        TableColumn<ServiceItem, String> idColumn =
                new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.valueOf(cell.getValue().getId())));

        TableColumn<ServiceItem, String> nameColumn =
                new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));

        TableColumn<ServiceItem, String> descriptionColumn =
                new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getDescription()));

        TableColumn<ServiceItem, String> priceColumn =
                new TableColumn<>("Price (SEK)");
        priceColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.valueOf(cell.getValue().getPrice())));

        TableColumn<ServiceItem, String> timeColumn =
                new TableColumn<>("Estimated time (min)");
        timeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        String.valueOf(cell.getValue().getEstimatedMinutes())));


        idColumn.setPrefWidth(50);
        nameColumn.setPrefWidth(150);
        descriptionColumn.setPrefWidth(300);
        priceColumn.setPrefWidth(110);
        timeColumn.setPrefWidth(160);

        table.getColumns().add(idColumn);
        table.getColumns().add(nameColumn);
        table.getColumns().add(descriptionColumn);
        table.getColumns().add(priceColumn);
        table.getColumns().add(timeColumn);


        table.setPlaceholder(new Label("No services found."));

        //hämtar aktuella tjänster varje gång vyn öppnas
        table.setItems(
                FXCollections.observableArrayList(Database.getServiceItems())
        );

        Label durationLabel = new Label("Estimated time (minutes):");
        TextField durationField = new TextField();
        durationField.setPromptText("Select a service and enter minutes");

        Button saveButton = new Button("Save duration");
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        ServiceItemRepository repository = new ServiceItemRepository();

// Visar tidsåtgången för tjänsten som användaren väljer.
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    feedbackLabel.setText("");

                    if (selected == null) {
                        durationField.clear();
                    } else {
                        durationField.setText(
                                String.valueOf(selected.getEstimatedMinutes())
                        );
                    }
                }
        );

        saveButton.setOnAction(event -> {
            ServiceItem selected = table.getSelectionModel().getSelectedItem();

            if (selected == null) {
                feedbackLabel.setText("Please select a service.");
                return;
            }

            int minutes;

            try {
                minutes = Integer.parseInt(durationField.getText().trim());
            } catch (NumberFormatException exception) {
                feedbackLabel.setText("Enter a whole number greater than 0.");
                return;
            }

            if (minutes <= 0) {
                feedbackLabel.setText("The duration must be greater than 0.");
                return;
            }

            // Behåller det gamla värdet om sparandet misslyckas.
            int previousMinutes = selected.getEstimatedMinutes();
            selected.setEstimatedMinutes(minutes);

            try {
                repository.save(selected);
            } catch (RuntimeException exception) {
                selected.setEstimatedMinutes(previousMinutes);
                table.refresh();

                feedbackLabel.setText(
                        "The duration could not be saved. Please try again."
                );
                exception.printStackTrace();
                return;
            }

            table.refresh();
            feedbackLabel.setText("Duration saved.");
        });

        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(
                title,
                table,
                durationLabel,
                durationField,
                saveButton,
                feedbackLabel
        );
    }
}