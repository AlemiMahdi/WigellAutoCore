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
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.ListCell;

public class ProcessPaymentView {

    private final GarageSystem garageSystem = new GarageSystem();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    // Översätter visningstexten men behåller betalningstypens kodvärde.
    private ListCell<String> createPaymentTypeCell(LanguageManager language) {
        return new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                textProperty().unbind();

                if (empty || item == null) {
                    setText(null);
                } else {
                    textProperty().bind(language.text("payment.type." + item));
                }
            }
        };
    }

    public VBox getView() {

        LanguageManager language = LanguageManager.getInstance();

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

        paymentTypeBox.setCellFactory(list -> createPaymentTypeCell(language));
        paymentTypeBox.setButtonCell(createPaymentTypeCell(language));

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

        // Uppdaterar formulärets texter direkt vid språkbyte.
        title.textProperty().bind(language.text("processPayment.title"));
        invoicesLabel.textProperty().bind(language.text("processPayment.invoices"));
        invoiceIdLabel.textProperty().bind(language.text("processPayment.invoiceId"));
        invoiceIdField.promptTextProperty().bind(
                language.text("processPayment.invoicePrompt")
        );
        paymentTypeLabel.textProperty().bind(language.text("processPayment.type"));
        paymentTypeBox.promptTextProperty().bind(
                language.text("processPayment.typePrompt")
        );
        processButton.textProperty().bind(language.text("processPayment.button"));

        messageLabel.setWrapText(true);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("invoices.empty"));
        invoiceList.setPlaceholder(emptyLabel);

        processButton.setOnAction(event -> {

            int invoiceId;

            try {

                invoiceId = Integer.parseInt(
                        invoiceIdField.getText()
                );

            } catch (NumberFormatException e) {

                messageLabel.textProperty().bind(
                        language.text("processPayment.invalidId")
                );

                return;
            }


            String paymentType =
                    paymentTypeBox.getValue();

            if (paymentType == null) {

                messageLabel.textProperty().bind(
                        language.text("processPayment.selectType")
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

                messageLabel.textProperty().bind(
                        language.text("processPayment.processError")
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

            messageLabel.textProperty().bind(
                    language.text("processPayment.saveError")
            );

        exception.printStackTrace();

        return;
        }

        if (payment.isSuccessful()) {

            messageLabel.textProperty().bind(
                    language.text("processPayment.success")
            );

                invoiceIdField.clear();
                paymentTypeBox.setValue(null);

                invoiceList.refresh();

        } else {
            messageLabel.textProperty().bind(
                    language.text("processPayment.failed")
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