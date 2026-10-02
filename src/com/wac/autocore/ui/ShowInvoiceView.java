package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;


public class ShowInvoiceView {

    public static VBox build() {
        LanguageManager language = LanguageManager.getInstance();

        // "+ New invoice" byter sida via Navigator, så att menyn också
        // markerar "Create invoice" som aktiv (precis som vid ett menyklick)
        Button newInvoiceButton = UiKit.primaryButton("+ New invoice");
        newInvoiceButton.textProperty().bind(language.text("invoices.new"));
        newInvoiceButton.setOnAction(event -> Navigator.goTo("create-invoice"));

        TableView<Invoice> table = new TableView<>();
        Label emptyLabel = UiKit.emptyText("");
        emptyLabel.textProperty().bind(language.text("invoices.empty"));
        table.setPlaceholder(emptyLabel);

        // Arbetsorder visas som "WO-<id>" som i designen
        TableColumn<Invoice, String> workOrderCol = new TableColumn<>("Work order");
        workOrderCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(workOrderCode(cellData.getValue().getWorkOrderId())));

        TableColumn<Invoice, String> invoiceDateCol = new TableColumn<>("Date");
        invoiceDateCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getInvoiceDate())));
        invoiceDateCol.getStyleClass().add("cell-muted");

        TableColumn<Invoice, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getAmount())));
        amountCol.getStyleClass().add("cell-right");

        TableColumn<Invoice, String> discountCol = new TableColumn<>("Discount");
        discountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getDiscount())));
        discountCol.getStyleClass().addAll("cell-right", "cell-muted");

        // Totalen är det viktigaste beloppet – därför fetstil
        TableColumn<Invoice, String> totalAmountCol = new TableColumn<>("Total");
        totalAmountCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getTotalAmount())));
        totalAmountCol.getStyleClass().addAll("cell-right", "cell-strong");

        // Modellen har en boolean (paid) – badgen behöver texten "Paid"/"Unpaid"
        TableColumn<Invoice, String> paidCol = new TableColumn<>("Status");
        paidCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isPaid() ? "Paid" : "Unpaid"));
        paidCol.setCellFactory(UiKit.<Invoice>badgeCells());

        workOrderCol.textProperty().bind(language.text("invoices.workOrderId"));
        invoiceDateCol.textProperty().bind(language.text("invoices.date"));
        amountCol.textProperty().bind(language.text("invoices.amount"));
        discountCol.textProperty().bind(language.text("invoices.discount"));
        totalAmountCol.textProperty().bind(language.text("invoices.total"));
        paidCol.textProperty().bind(language.text("invoices.status"));

        table.getColumns().addAll(workOrderCol, invoiceDateCol, amountCol, discountCol, totalAmountCol, paidCol);

        // Kopia av listan, vänd så att nyaste fakturan hamnar överst.
        // (Själva listan i Database ändras inte.)
        ObservableList<Invoice> data = FXCollections.observableArrayList(Database.getInvoices());
        Collections.reverse(data);
        table.setItems(data);

        UiKit.styleTable(table);

        TableView<InvoiceLine> lineTable = new TableView<>();

        TableColumn<InvoiceLine, String> serviceCol = new TableColumn<>("Service");
        serviceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getServiceName()));

        TableColumn<InvoiceLine, String> priceLineCol = new TableColumn<>("Price");
        priceLineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getPrice())));
        priceLineCol.getStyleClass().add("cell-right");

        TableColumn<InvoiceLine, String> discountLineCol = new TableColumn<>("Discount");
        discountLineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getDiscount())));
        discountLineCol.getStyleClass().addAll("cell-right", "cell-muted");

        TableColumn<InvoiceLine, String> finalPriceLineCol = new TableColumn<>("Final price");
        finalPriceLineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getFinalPrice())));
        finalPriceLineCol.getStyleClass().addAll("cell-right", "cell-strong");

        serviceCol.textProperty().bind(language.text("invoiceLine.service"));
        priceLineCol.textProperty().bind(language.text("invoiceLine.price"));
        discountLineCol.textProperty().bind(language.text("invoiceLine.discount"));
        finalPriceLineCol.textProperty().bind(language.text("invoiceLine.finalPrice"));

        lineTable.getColumns().addAll(serviceCol, priceLineCol, discountLineCol, finalPriceLineCol);

        UiKit.styleTable(lineTable);

        // När användaren klickar på en faktura visas dess rader i radtabellen.
        // addListener körs automatiskt varje gång valet ändras (Observer-mönstret).
        // newInvoice är null när ingen faktura är vald, därför kontrolleras det först.
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldInvoice, newInvoice) -> {
            if (newInvoice != null) {
                ObservableList<InvoiceLine> lines = FXCollections.observableArrayList(newInvoice.getLines());
                lineTable.setItems(lines);
            }
        });

        VBox view = new VBox(24, UiKit.pageHeader(language.text("invoices.title"), newInvoiceButton), table, lineTable);
        return view;
    }

    /** "WO-<id>" – samma sätt att skriva arbetsorder-nummer på alla fakturasidor. */
    public static String workOrderCode(int workOrderId) {
        return "WO-" + workOrderId;
    }

    /**
     * Gör om ett belopp till text i svenskt format med "SEK" på slutet.
     * Mellanslag (' ') mellan tusental, komma (',') som decimaltecken
     * och decimaler bara när de behövs.
     * # betyder "visa siffran om den finns", och 0 betyder "visa alltid en siffra".
     * Exempel: 3495.0 blir "3 495 SEK" och 1305.5 blir "1 305,5 SEK".
     *
     * Används av alla vyer som visar belopp, så att de ser likadana ut.
     */
    public static String formatSek(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        // Mallen för hur talet ska se ut.
        DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
        return format.format(amount) + " SEK";
    }
}
