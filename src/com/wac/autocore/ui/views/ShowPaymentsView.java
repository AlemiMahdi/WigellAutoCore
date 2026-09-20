package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Payment;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class ShowPaymentsView {

    public VBox getView() {

        Label title = new Label("PAYMENTS");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        TableView<Payment> table = new TableView<>();


        // ID
        TableColumn<Payment, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<Payment, Integer>("id")
        );


        // Invoice ID
        TableColumn<Payment, Integer> invoiceIdColumn =
                new TableColumn<>("Invoice ID");

        invoiceIdColumn.setCellValueFactory(
                new PropertyValueFactory<Payment, Integer>("invoiceId")
        );


        // Amount
        TableColumn<Payment, Double> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(
                new PropertyValueFactory<Payment, Double>("amount")
        );


        // Payment type
        TableColumn<Payment, String> paymentTypeColumn =
                new TableColumn<>("Payment type");

        paymentTypeColumn.setCellValueFactory(
                new PropertyValueFactory<Payment, String>("paymentType")
        );


        // Payment date
        TableColumn<Payment, LocalDateTime> paymentDateColumn =
                new TableColumn<>("Date");

        paymentDateColumn.setCellValueFactory(
                new PropertyValueFactory<Payment, LocalDateTime>("paymentDate")
        );


        // Successful
        TableColumn<Payment, String> successfulColumn =
                new TableColumn<>("Successful");

        successfulColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().isSuccessful()
                                ? "Yes"
                                : "No"
                )
        );


        table.getColumns().addAll(
                idColumn,
                invoiceIdColumn,
                amountColumn,
                paymentTypeColumn,
                paymentDateColumn,
                successfulColumn
        );


        ObservableList<Payment> payments =
                FXCollections.observableArrayList(
                        Database.getPayments()
                );

        table.setItems(payments);

        table.setPlaceholder(
                new Label("No payments found.")
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