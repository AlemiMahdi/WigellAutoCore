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
import com.wac.autocore.ui.language.LanguageManager;

public class ShowPaymentsView {

    public VBox getView() {

        LanguageManager language = LanguageManager.getInstance();

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

        paymentTypeColumn.setCellValueFactory(cell ->
                language.text("payment.type." + cell.getValue().getPaymentType())
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

        successfulColumn.setCellValueFactory(cell ->
                language.text(
                        cell.getValue().isSuccessful() ? "common.yes" : "common.no"
                )
        );

        title.textProperty().bind(language.text("payments.title"));
        idColumn.textProperty().bind(language.text("payments.id"));
        invoiceIdColumn.textProperty().bind(language.text("payments.invoiceId"));
        amountColumn.textProperty().bind(language.text("payments.amount"));
        paymentTypeColumn.textProperty().bind(language.text("payments.type"));
        paymentDateColumn.textProperty().bind(language.text("payments.date"));
        successfulColumn.textProperty().bind(language.text("payments.successful"));

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

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("payments.empty"));
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