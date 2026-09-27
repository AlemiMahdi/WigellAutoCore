package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ShowInvoiceView;
import com.wac.autocore.ui.UiKit;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import com.wac.autocore.repository.PaymentRepository;

public class ProcessPaymentView {

    private final GarageSystem garageSystem = new GarageSystem();
    private final PaymentRepository paymentRepository = new PaymentRepository();

    public VBox getView() {

        // --- Faktura ---
        // Visar alla fakturor (som konsolversionen), med belopp och om den är betald
        ComboBox<Invoice> invoiceBox = new ComboBox<>();
        invoiceBox.setConverter(new StringConverter<Invoice>() {
            @Override
            public String toString(Invoice invoice) {
                return invoice == null ? "" : describeInvoice(invoice);
            }

            @Override
            public Invoice fromString(String text) {
                return null; // Används inte – listan går inte att skriva i
            }
        });
        invoiceBox.getItems().addAll(Database.getInvoices());
        invoiceBox.setPromptText(
                invoiceBox.getItems().isEmpty() ? "No invoices yet" : "Select invoice"
        );
        UiKit.keepPromptWhenCleared(invoiceBox);


        // --- Belopp ---
        // GarageSystem drar alltid fakturans totalbelopp,
        // därför visar fältet bara det och går inte att ändra.
        TextField amountField = new TextField();
        amountField.setEditable(false);
        amountField.setPromptText("Select an invoice");
        amountField.getStyleClass().add("readonly-field");

        invoiceBox.valueProperty().addListener((observable, oldValue, selected) -> {
            if (selected == null) {
                amountField.clear();
            } else {
                amountField.setText(ShowInvoiceView.formatSek(selected.getTotalAmount()));
            }
        });


        // --- Betalsätt ---
        // Värdena skickas vidare till GarageSystem exakt som förut (CARD/SWISH/CASH),
        // converter:n ändrar bara hur de visas ("Card").
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
        paymentTypeBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String type) {
                return type == null ? "" : formatPaymentType(type);
            }

            @Override
            public String fromString(String text) {
                return text;
            }
        });
        UiKit.keepPromptWhenCleared(paymentTypeBox);


        // Meddelande till användaren
        Label messageLabel = UiKit.feedbackLabel();

        if (invoiceBox.getItems().isEmpty()) {
            UiKit.showInfo(messageLabel, "There are no invoices to pay yet.");
        }


        Button processButton =
                UiKit.successButton("Process payment");


        processButton.setOnAction(event -> {

            Invoice selectedInvoice = invoiceBox.getValue();

            if (selectedInvoice == null) {

                UiKit.showError(messageLabel,
                        "Please select an invoice."
                );

                return;
            }

            int invoiceId = selectedInvoice.getId();


            String paymentType =
                    paymentTypeBox.getValue();

            if (paymentType == null) {

                UiKit.showError(messageLabel,
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

            Payment payment = garageSystem.processPayment(invoiceId, paymentType);


            if (payment == null) {

                UiKit.showError(messageLabel,
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

                refreshInvoiceTexts(invoiceBox);

                UiKit.showError(messageLabel,
                        "Payment could not be saved to the database."
                );

                exception.printStackTrace();

                return;
            }

            if (payment.isSuccessful()) {

                UiKit.showSuccess(messageLabel,
                        "Payment completed successfully."
                );

                invoiceBox.setValue(null);
                paymentTypeBox.setValue(null);


                // Rita om listan så att "paid" syns direkt på fakturan
                refreshInvoiceTexts(invoiceBox);

            } else {

                UiKit.showError(messageLabel, "Payment failed.");


            }
        });


        VBox form = UiKit.formContainer(
                UiKit.pageHeader("Process payment", null),
                UiKit.formField("Invoice", invoiceBox),
                UiKit.formField("Amount", amountField),
                UiKit.formField("Payment type", paymentTypeBox),
                processButton,
                messageLabel
        );

        // Formulärkolumnen ska ligga centrerad i innehållsytan
        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);

        return view;
    }

    /**
     * "CARD" -> "Card". Används även i ShowPaymentsView så att betalsätt ser likadana ut.
     */
    public static String formatPaymentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "—";
        }
        String lower = type.trim().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // "WO-1 invoice · 3 495 SEK unpaid"
    private static String describeInvoice(Invoice invoice) {
        return ShowInvoiceView.workOrderCode(invoice.getWorkOrderId()) + " invoice · "
                + ShowInvoiceView.formatSek(invoice.getTotalAmount())
                + (invoice.isPaid() ? " paid" : " unpaid");
    }

    // En ComboBox ritar inte om texterna av sig själv när ett objekt ändras
    // (här: paid blev true). Genom att lägga in samma objekt igen uppdateras de.
    private static void refreshInvoiceTexts(ComboBox<Invoice> invoiceBox) {
        invoiceBox.getItems().setAll(Database.getInvoices());
    }

}
