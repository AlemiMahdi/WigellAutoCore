package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.service.GarageSystem;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class CompleteWorkOrderView {
    public static VBox build(){

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
                statusLabel.setText("Cannot complete work order. Has to be on status: IN_PROGRESS. (current status: " + foundOrder.getStatus() + ")");
            } else {
                garageSystem.completeWorkOrder(workOderId);
                statusLabel.setText("Work order completed");
                workOrderField.clear();
            }


        });

        VBox root = new VBox(15, form, completeButton, statusLabel);
        root.setPadding(new Insets(20));

        return root;
    }
}
