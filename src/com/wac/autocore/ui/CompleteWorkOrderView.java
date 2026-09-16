package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CompleteWorkOrderView {
    public static VBox build(){

        TableView<WorkOrder> tableView = new TableView<>();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<WorkOrder, Integer> idCol = new TableColumn<>("Work order Id");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<WorkOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableView.getColumns().addAll(idCol, statusCol);

        ObservableList<WorkOrder> data = FXCollections.observableArrayList(Database.getWorkOrders());
        tableView.setItems(data);

        GarageSystem garageSystem = new GarageSystem();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        Label workOrderIdLabel = new Label("Work order Id:");
        TextField workOrderField = new TextField();

        form.add(workOrderIdLabel, 0, 0);
        form.add(workOrderField,1, 0);

        Button completeButton = new Button("Complete Work order");
        Label statusLabel = new Label();

        completeButton.setOnAction(actionEvent -> {
            int workOderId;

            try {
                workOderId = Integer.parseInt(workOrderField.getText());
            } catch (NumberFormatException e) {
                statusLabel.setText("Work order ID has to be a number.");
                return;
            }

            WorkOrder foundOrder = null;

            for(WorkOrder order : Database.getWorkOrders()){
                if(order.getId() == workOderId) {
                    foundOrder = order;
                    break;
                }
            }
            if(foundOrder == null) {
                statusLabel.setText("Work order does not exist");
            } else if(!foundOrder.getStatus().equals("IN_PROGRESS")) {
                statusLabel.setText("Cannot complete work order. Has to be on status: IN_PROGRESS.");
            } else {
                garageSystem.completeWorkOrder(workOderId);
                statusLabel.setText("Work order completed");
                workOrderField.clear();
            }


        });

        VBox root = new VBox(15, tableView ,form, completeButton, statusLabel);
        root.setPadding(new Insets(20));

        return root;
    }
}
