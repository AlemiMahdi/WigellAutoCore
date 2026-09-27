package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.GarageSystem;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import com.wac.autocore.repository.PaymentRepository;

public class ProcessPaymentView {

    private final GarageSystem garageSystem = new GarageSystem();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public VBox getView() {

        Label title = new Label("PROCESS PAYMENT");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );


        // Visar invoices precis som konsolversionen
        Label invoicesLabel = new Label("Available invoices:");

        ListView<Invoice> invoiceList = new ListView<>(
                FXCollections.observableArrayList(
                        Database.getInvoices()
                )
        );

        invoiceList.setPrefHeight(180);


        // Invoice ID
        Label invoiceIdLabel = new Label("Invoice ID:");

        TextField invoiceIdField = new TextField();

        invoiceIdField.setPromptText("Enter invoice ID");


        // Payment type
        Label paymentTypeLabel = new Label("Payment type:");

        ComboBox<String> paymentTypeBox =
                new ComboBox<>();

        paymentTypeBox.getItems().addAll(
                "CARD",
                "SWISH",
                "CASH"
        );

        paymentTypeBox.setPromptText(
                "Select payment type"
        );


        // Meddelande till användaren
        Label messageLabel = new Label();


        Button processButton =
                new Button("Process payment");


        processButton.setOnAction(event -> {

            int invoiceId;

            try {

                invoiceId = Integer.parseInt(
                        invoiceIdField.getText()
                );

            } catch (NumberFormatException e) {

                messageLabel.setText(
                        "Please enter a valid invoice ID."
                );

                return;
            }


            String paymentType =
                    paymentTypeBox.getValue();

            if (paymentType == null) {

                messageLabel.setText(
                        "Please select a payment type."
                );

                return;
            }

        Invoice invoice = Database.getInvoices().stream()
                .filter(existingInvoice ->
                        existingInvoice.getId() == invoiceId)
                .findFirst()
                .orElse(null);

        boolean previousPaidStatus = invoice != null && invoice.isPaid();

        Payment payment = garageSystem.processPayment( invoiceId, paymentType );


            if (payment == null) {

                messageLabel.setText(
                        "Payment could not be processed. " +
                                "Check invoice ID or if the invoice is already paid."
                );

                return;
            }

        try {

        paymentRepository.savePaymentAndInvoice(
                payment,
                invoice
        );

        } catch (RuntimeException exception) {

        // Återställ ändringarna som GarageSystem gjorde i minnet.
        Database.getPayments().remove(payment);
        invoice.setPaid(previousPaidStatus);

        invoiceList.refresh();

        messageLabel.setText(
                "Payment could not be saved to the database."
        );

        exception.printStackTrace();

        return;
        }

        if (payment.isSuccessful()) {

                messageLabel.setText(
                        "Payment completed successfully."
                );

                invoiceIdField.clear();
                paymentTypeBox.setValue(null);

                invoiceList.refresh();

        } else {
                messageLabel.setText(
                        "Payment failed."
                );
        }
        });


        VBox view = new VBox(10);

        view.setPadding(new Insets(10));

        view.getChildren().addAll(
                title,
                invoicesLabel,
                invoiceList,
                invoiceIdLabel,
                invoiceIdField,
                paymentTypeLabel,
                paymentTypeBox,
                processButton,
                messageLabel
        );

        return view;
    }
}