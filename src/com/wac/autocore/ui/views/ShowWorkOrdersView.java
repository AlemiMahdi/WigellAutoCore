package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import com.wac.autocore.ui.language.LanguageManager;

public class ShowWorkOrdersView {

    public VBox getView() {

        LanguageManager language = LanguageManager.getInstance();

        Label title = new Label("WORK ORDERS");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        TableView<WorkOrder> table = new TableView<>();


        // ID
        TableColumn<WorkOrder, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<WorkOrder, Integer>("id")
        );


        // Booking ID
        TableColumn<WorkOrder, Integer> bookingIdColumn =
                new TableColumn<>("Booking ID");

        bookingIdColumn.setCellValueFactory(
                new PropertyValueFactory<WorkOrder, Integer>("bookingId")
        );


        // Mechanic ID
        TableColumn<WorkOrder, Integer> mechanicIdColumn =
                new TableColumn<>("Mechanic ID");

        mechanicIdColumn.setCellValueFactory(
                new PropertyValueFactory<WorkOrder, Integer>("mechanicId")
        );


// Services
        TableColumn<WorkOrder, String> servicesColumn =
                new TableColumn<>("Services");

        servicesColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().getServiceItemIds().toString()
                )
        );

// Status
        TableColumn<WorkOrder, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(cell ->
                language.text("workOrder.status." + cell.getValue().getStatus())
        );
// Uppdaterar rubrikerna direkt vid språkbyte.
        title.textProperty().bind(language.text("workOrders.title"));
        idColumn.textProperty().bind(language.text("workOrders.id"));
        bookingIdColumn.textProperty().bind(language.text("workOrders.bookingId"));
        mechanicIdColumn.textProperty().bind(language.text("workOrders.mechanicId"));
        servicesColumn.textProperty().bind(language.text("workOrders.services"));
        statusColumn.textProperty().bind(language.text("workOrders.status"));

        table.getColumns().addAll(
                idColumn,
                bookingIdColumn,
                mechanicIdColumn,
                servicesColumn,
                statusColumn
        );


        ObservableList<WorkOrder> workOrders =
                FXCollections.observableArrayList(
                        Database.getWorkOrders()
                );

        table.setItems(workOrders);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("workOrders.empty"));
        table.setPlaceholder(emptyLabel);


        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );


        VBox view = new VBox(15);

        view.setPadding(new Insets(10));

        view.getChildren().addAll(
                title,
                table
        );

        return view;
    }
}