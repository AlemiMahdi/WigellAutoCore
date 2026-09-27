package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.Label;


public class ShowInvoiceView {

    public static TableView<Invoice> build(){

        LanguageManager language = LanguageManager.getInstance();

        TableView<Invoice> table = new TableView<>();

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Invoice, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Invoice, Integer> workOrderCol = new TableColumn<>("Work order-Id");
        workOrderCol.setCellValueFactory(new PropertyValueFactory<>("workOrderId"));

        TableColumn<Invoice, LocalDate> invoiceDateCol = new TableColumn<>("Invoice date");
        invoiceDateCol.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));

        TableColumn<Invoice,Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Invoice, Double> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discount"));

        TableColumn<Invoice, Double> totalAmountCol = new TableColumn<>("Total amount");
        totalAmountCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        TableColumn<Invoice, String> paidCol = new TableColumn<>("Paid");
        paidCol.setCellValueFactory(cell ->
                language.text(cell.getValue().isPaid() ? "common.yes" : "common.no")
        );

        // Uppdaterar rubrikerna direkt vid språkbyte.
        idCol.textProperty().bind(language.text("invoices.id"));
        workOrderCol.textProperty().bind(language.text("invoices.workOrderId"));
        invoiceDateCol.textProperty().bind(language.text("invoices.date"));
        amountCol.textProperty().bind(language.text("invoices.amount"));
        discountCol.textProperty().bind(language.text("invoices.discount"));
        totalAmountCol.textProperty().bind(language.text("invoices.total"));
        paidCol.textProperty().bind(language.text("invoices.paid"));

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("invoices.empty"));
        table.setPlaceholder(emptyLabel);

        table.getColumns().addAll(idCol, workOrderCol, invoiceDateCol, amountCol, discountCol, totalAmountCol, paidCol);

        ObservableList<Invoice> data = FXCollections.observableArrayList(Database.getInvoices());
        table.setItems(data);

        return table;
    }
}
