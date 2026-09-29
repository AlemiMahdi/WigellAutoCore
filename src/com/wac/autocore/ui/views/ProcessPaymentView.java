package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.ShowInvoiceView;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import com.wac.autocore.repository.PaymentRepository;

public class ProcessPaymentView {

    private final LanguageManager language = LanguageManager.getInstance();
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
        invoiceBox.promptTextProperty().bind(language.text(
                invoiceBox.getItems().isEmpty() ? "processPayment.noInvoicesPrompt" : "processPayment.invoiceSelectPrompt"
        ));
        UiKit.keepPromptWhenCleared(invoiceBox);


        // --- Belopp ---
        // GarageSystem drar alltid fakturans totalbelopp,
        // därför visar fältet bara det och går inte att ändra.
        TextField amountField = new TextField();
        amountField.setEditable(false);
        amountField.promptTextProperty().bind(language.text("processPayment.invoiceSelectPrompt"));
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

        paymentTypeBox.promptTextProperty().bind(language.text("processPayment.typePrompt"));

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
            UiKit.showInfo(messageLabel, language.text("processPayment.noInvoices").get());
        }


        Button processButton =
                UiKit.successButton("Process payment");
        processButton.textProperty().bind(language.text("processPayment.button"));


        processButton.setOnAction(event -> {

            Invoice selectedInvoice = invoiceBox.getValue();

            if (selectedInvoice == null) {

                UiKit.showError(messageLabel,
                        language.text("processPayment.selectInvoice").get()
                );

                return;
            }

            int invoiceId = selectedInvoice.getId();


            String paymentType =
                    paymentTypeBox.getValue();

            if (paymentType == null) {

                UiKit.showError(messageLabel,
                        language.text("processPayment.selectType").get()
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
                        language.text("processPayment.processError").get()
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
                        language.text("processPayment.saveError").get()
                );

                exception.printStackTrace();

                return;
            }

            if (payment.isSuccessful()) {

                UiKit.showSuccess(messageLabel,
                        language.text("processPayment.success").get()
                );

                invoiceBox.setValue(null);
                paymentTypeBox.setValue(null);


                // Rita om listan så att "paid" syns direkt på fakturan
                refreshInvoiceTexts(invoiceBox);

            } else {

                UiKit.showError(messageLabel, language.text("processPayment.failed").get());


            }
        });


        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("processPayment.title"), null),
                UiKit.formField(language.text("payments.invoice"), invoiceBox),
                UiKit.formField(language.text("invoices.amount"), amountField),
                UiKit.formField(language.text("payments.type"), paymentTypeBox),
                processButton,
                messageLabel
        );

        // Formulärkolumnen ska ligga centrerad i innehållsytan
        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);

        return view;
    }

    /**
     * "CARD" -> "Kort"/"Card" via språkfilen. Används även i ShowPaymentsView så att betalsätt ser likadana ut.
     */
    public static String formatPaymentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "—";
        }
        return LanguageManager.getInstance()
                .text("payment.type." + type.trim().toUpperCase()).get();
    }

    // "WO-1 faktura · 3 495 SEK · Obetald"
    private static String describeInvoice(Invoice invoice) {
        LanguageManager language = LanguageManager.getInstance();
        return ShowInvoiceView.workOrderCode(invoice.getWorkOrderId()) + " "
                + language.text("payments.invoiceSuffix").get() + " · "
                + ShowInvoiceView.formatSek(invoice.getTotalAmount()) + " · "
                + language.text(invoice.isPaid() ? "badge.PAID" : "badge.UNPAID").get();
    }

    // En ComboBox ritar inte om texterna av sig själv när ett objekt ändras
    // (här: paid blev true). Genom att lägga in samma objekt igen uppdateras de.
    private static void refreshInvoiceTexts(ComboBox<Invoice> invoiceBox) {
        invoiceBox.getItems().setAll(Database.getInvoices());
    }

}
