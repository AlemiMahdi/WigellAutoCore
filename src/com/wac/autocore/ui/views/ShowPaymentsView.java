package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.ui.ShowInvoiceView;
import com.wac.autocore.ui.UiKit;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

        TableView<Payment> table = new TableView<>();

        table.setPlaceholder(
                UiKit.emptyText("No payments found.")
        );


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
                UiKit.pageHeader("Payments", null),
                table
        );

        return view;
    }

    // Letar upp fakturan för att kunna visa dess arbetsorder-nummer.
    // Hittas den inte visas faktura-id:t i stället.
    private static String describeInvoice(int invoiceId) {
        for (Invoice invoice : Database.getInvoices()) {
            if (invoice.getId() == invoiceId) {
                return ShowInvoiceView.workOrderCode(invoice.getWorkOrderId()) + " invoice";
            }
        }
        return "Invoice #" + invoiceId;
    }
}
