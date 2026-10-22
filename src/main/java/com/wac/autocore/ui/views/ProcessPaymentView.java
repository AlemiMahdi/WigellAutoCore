package com.wac.autocore.ui.views;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.PaymentService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.ui.ErrorHandler;

import java.util.logging.Logger;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;


public class ProcessPaymentView {

    private static final Logger LOGGER =
            Logger.getLogger(ProcessPaymentView.class.getName());

    private final LanguageManager language = LanguageManager.getInstance();

    public VBox getView(PaymentService paymentService) {

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
        refreshInvoices(invoiceBox);

        invoiceBox.promptTextProperty().bind(language.text(
                invoiceBox.getItems().isEmpty() ? "processPayment.noInvoicesPrompt" : "processPayment.invoiceSelectPrompt"
        ));
        UiKit.keepPromptWhenCleared(invoiceBox);


        // --- Belopp ---
        // PaymentService drar alltid fakturans totalbelopp,
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
                return type == null ? "" : ViewHelpMethod.formatPaymentType(type);
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
            try {
                paymentService.processPayment(invoiceBox.getValue(), paymentTypeBox.getValue());
                UiKit.showSuccess(messageLabel, language.text("processPayment.success").get());
                invoiceBox.setValue(null);
                paymentTypeBox.setValue(null);
                refreshInvoices(invoiceBox);
            } catch (RuntimeException exception) {
                ErrorHandler.show(
                        messageLabel,
                        exception,
                        "processPayment.saveError"
                );
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

    // "WO-1 faktura · 3 495 SEK · Obetald"
    private static String describeInvoice(Invoice invoice) {
        LanguageManager language = LanguageManager.getInstance();
        return ShowInvoiceView.workOrderCode(invoice.getWorkOrderId()) + " "
                + language.text("payments.invoiceSuffix").get() + " · "
                + ShowInvoiceView.formatSek(invoice.getTotalAmount()) + " · "
                + language.text(invoice.isPaid() ? "badge.PAID" : "badge.UNPAID").get();
    }

    private void refreshInvoices(ComboBox<Invoice> invoiceBox) {
        invoiceBox.getItems().setAll(
            UiKit.newestFirst(
                PaymentService.findUnpaidInvoices(),
                    Invoice::getId
            )
        );
    }
}
