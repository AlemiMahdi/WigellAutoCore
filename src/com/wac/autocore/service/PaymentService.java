package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.IPaymentRepository;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;


public class PaymentService {

    private final IPaymentRepository paymentRepository;
    public PaymentService(IPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

public void processPayment(ComboBox<Invoice> invoiceBox, ComboBox<String> paymentTypeBox, Label messageLabel, LanguageManager language) {
    Invoice selectedInvoice = invoiceBox.getValue();

    if (selectedInvoice == null) {
        UiKit.showError(messageLabel,
                language.text("processPayment.selectInvoice").get()
        );
        return;
    }

    //förhindra att faktura kan registreras igen
    if (selectedInvoice.isPaid()) {
        UiKit.showError(messageLabel, language.text("processPayment.alreadyPaid").get());
        return;
    }

    String paymentType = paymentTypeBox.getValue();
    if (paymentType == null) {
        UiKit.showError(messageLabel,
                language.text("processPayment.selectType").get()
        );
        return;
    }

    boolean previousPaidStatus = selectedInvoice.isPaid();

    Payment payment = new Payment(
            selectedInvoice.getId(), selectedInvoice.getTotalAmount(), paymentType
    );

    boolean successful = "CARD".equalsIgnoreCase(paymentType) ||
            "SWISH".equalsIgnoreCase(paymentType) ||
            "CASH".equalsIgnoreCase(paymentType);
    payment.setSuccessful(successful);

    // Om betalningen inte är godkänd, avbryt innan vi rör databasen
    if (!payment.isSuccessful()) {
        UiKit.showError(messageLabel, language.text("processPayment.failed").get());
        return;
    }
try {
    selectedInvoice.setPaid(true);
    paymentRepository.savePaymentAndInvoice(
                payment, selectedInvoice
        );
    Database.getPayments().add(payment);
    } catch (RuntimeException exception) {
        // Återställ ändringarna som Database gjorde i minnet.
        Database.getPayments().remove(payment);
        selectedInvoice.setPaid(previousPaidStatus);

        refreshInvoiceTexts(invoiceBox);
        UiKit.showError(messageLabel, language.text("processPayment.saveError").get());
        exception.printStackTrace();
        return;
    }

    if (payment.isSuccessful()) {
        UiKit.showSuccess(messageLabel,
                language.text("processPayment.success").get()
        );
        invoiceBox.getItems().remove(selectedInvoice);
        invoiceBox.setValue(null);
        paymentTypeBox.setValue(null);

        // Rita om listan så att "paid" syns direkt på fakturan
        refreshInvoiceTexts(invoiceBox);

    } else {
        UiKit.showError(messageLabel, language.text("processPayment.failed").get());
    }
}

    // En ComboBox ritar inte om texterna av sig själv när ett objekt ändras
    // (här: paid blev true). Genom att lägga in samma objekt igen uppdateras de.
    public void refreshInvoiceTexts(ComboBox<Invoice> invoiceBox) {
        invoiceBox.getItems().clear();
        for(Invoice invoice : Database.getInvoices()) {
            if(!invoice.isPaid()) {
                invoiceBox.getItems().add(invoice);
            }
        }
    }

    /**
     * "CARD" -> "Kort"/"Card" via språkfilen. Används även i ShowPaymentsView så att betalsätt ser likadana ut.
     */
    public String formatPaymentType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return "—";
        }
        return LanguageManager.getInstance()
                .text("payment.type." + type.trim().toUpperCase()).get();
    }

    //Gammal funktion för konsol appen
//    public static void processPayment() {
//
//        System.out.println();
//        System.out.println("=== PROCESS PAYMENT ===");
//
//        garageSystem.showInvoices();
//
//        int invoiceId = readInt("Invoice ID: ");
//
//        System.out.println();
//        System.out.println("Available payment types:");
//        System.out.println("CARD");
//        System.out.println("SWISH");
//        System.out.println("CASH");
//
//        System.out.print("Payment type: ");
//        String paymentType = scanner.nextLine();
//
//        processPayment(
//                invoiceId,
//                paymentType
//        );
//    }
    //Används inte, gamla konsol appens funktion
//    public void showPayments() {
//        System.out.println();
//        System.out.println("=== PAYMENTS ===");
//
//        if (Database.getPayments().isEmpty()) {
//            System.out.println("No payments found.");
//            return;
//        }
//
//        for (Payment payment : Database.getPayments()) {
//            System.out.println(payment);
//        }
//    }
    //DUBBELKOLLA findInvoice metoden sen om den behöver hämtas från annan klass
//    public static Payment oldProcessPayment(int invoiceId, String paymentType) {
//        Invoice invoice = findInvoice(invoiceId);
//
//        if (invoice == null) {
//            System.out.println("Invoice with ID " + invoiceId + " does not exist.");
//            return null;
//        }
//
//        if (invoice.isPaid()) {
//            System.out.println("Invoice has already been paid.");
//            return null;
//        }
//
//        Payment payment = new Payment(
//                invoiceId,
//                invoice.getTotalAmount(),
//                paymentType
//        );
//
//        boolean successful = false;
//
//        if (paymentType.equalsIgnoreCase("CARD")) {
//
//            System.out.println("Connecting directly to SuperCardPayment...");
//            System.out.println("Card payment approved.");
//            successful = true;
//
//        } else if (paymentType.equalsIgnoreCase("SWISH")) {
//
//            System.out.println("Calling Swish payment service...");
//            System.out.println("Swish payment approved.");
//            successful = true;
//
//        } else if (paymentType.equalsIgnoreCase("CASH")) {
//
//            System.out.println("Registering cash payment...");
//            successful = true;
//
//        } else {
//
//            System.out.println("Unknown payment type.");
//        }
//
//        payment.setSuccessful(successful);
//        Database.getPayments().add(payment);
//
//        if (successful) {
//            invoice.setPaid(true);
//
//            System.out.println("Payment completed successfully.");
//            System.out.println("Sending payment confirmation to customer...");
//            System.out.println("Confirmation sent.");
//        } else {
//            System.out.println("Payment failed.");
//        }
//
//        return payment;
//    }
}
