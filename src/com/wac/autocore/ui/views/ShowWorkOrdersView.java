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

public class ShowWorkOrdersView {

    public VBox getView() {

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

        servicesColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue()
                                .getServiceItemIds()
                                .toString()
                )
        );


        // Status
        TableColumn<WorkOrder, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<WorkOrder, String>("status")
        );


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

        table.setPlaceholder(
                new Label("No work orders found.")
        );

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