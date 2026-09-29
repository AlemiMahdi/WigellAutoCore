package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.ui.ShowInvoiceView;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class ShowPaymentsView {

    // Datum och klockslag utan sekunder, t.ex. "2026-09-18 14:02"
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public VBox getView() {
        LanguageManager language = LanguageManager.getInstance();

        TableView<Payment> table = new TableView<>();

        Label emptyLabel = UiKit.emptyText("");
        emptyLabel.textProperty().bind(language.text("payments.empty"));
        table.setPlaceholder(emptyLabel);



        // Invoice – visas som "WO-1 invoice" så man ser vilken arbetsorder det gäller
        TableColumn<Payment, String> invoiceColumn =
                new TableColumn<>("Invoice");

        invoiceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        describeInvoice(cellData.getValue().getInvoiceId())
                )
        );

        // Amount – fetstil och högerställd så beloppen står under varandra
        TableColumn<Payment, String> amountColumn =
                new TableColumn<>("Amount");

        amountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        ShowInvoiceView.formatSek(cellData.getValue().getAmount())
                )
        );
        amountColumn.getStyleClass().addAll("cell-right", "cell-strong");


        // Payment type – "CARD" visas som "Card"
        TableColumn<Payment, String> paymentTypeColumn =
                new TableColumn<>("Type");

        paymentTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        ProcessPaymentView.formatPaymentType(cellData.getValue().getPaymentType())
                )
        );
        paymentTypeColumn.getStyleClass().add("cell-muted");


        // Payment date
        TableColumn<Payment, String> paymentDateColumn =
                new TableColumn<>("Date");

        paymentDateColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getPaymentDate() == null
                                ? "—"
                                : cellData.getValue().getPaymentDate().format(DATE_TIME_FORMAT)
                )
        );
        paymentDateColumn.getStyleClass().add("cell-muted");


        // Status – modellen har en boolean, badgen vill ha text
        TableColumn<Payment, String> successfulColumn =
                new TableColumn<>("Status");

        successfulColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().isSuccessful()
                                ? "Successful"
                                : "Failed"
                )
        );
        successfulColumn.setCellFactory(UiKit.<Payment>badgeCells());

        invoiceColumn.textProperty().bind(language.text("payments.invoice"));
        amountColumn.textProperty().bind(language.text("payments.amount"));
        paymentTypeColumn.textProperty().bind(language.text("payments.type"));
        paymentDateColumn.textProperty().bind(language.text("payments.date"));
        successfulColumn.textProperty().bind(language.text("payments.status"));

        table.getColumns().addAll(
                invoiceColumn,
                amountColumn,
                paymentTypeColumn,
                paymentDateColumn,
                successfulColumn
        );


        // Kopia av listan, vänd så att senaste betalningen hamnar överst
        ObservableList<Payment> payments =
                FXCollections.observableArrayList(
                        Database.getPayments()
                );
        Collections.reverse(payments);

        table.setItems(payments);

        UiKit.styleTable(table);


        VBox view = new VBox(24);

        view.getChildren().addAll(
                UiKit.pageHeader(language.text("payments.title"), null),
                table
        );

        return view;
    }

    // Letar upp fakturan för att kunna visa dess arbetsorder-nummer.
    // Hittas den inte visas faktura-id:t i stället.
    private static String describeInvoice(int invoiceId) {
        LanguageManager language = LanguageManager.getInstance();
        for (Invoice invoice : Database.getInvoices()) {
            if (invoice.getId() == invoiceId) {
                return ShowInvoiceView.workOrderCode(invoice.getWorkOrderId()) + " "
                        + language.text("payments.invoiceSuffix").get();
            }
        }
        return language.text("payments.invoiceNumber").get() + invoiceId;
    }
}
