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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

        Label headingLabel = new Label();
        headingLabel.getStyleClass().add("card-title");

        Label dateTitle = new Label();
        dateTitle.textProperty().bind(language.text("invoices.date"));
        dateTitle.getStyleClass().add("row-subtitle");
        Label dateValue = new Label();
        dateValue.getStyleClass().add("row-title");
        VBox dateBox = new VBox(2, dateTitle, dateValue);

        Label workOrderTitle = new Label();
        workOrderTitle.textProperty().bind(language.text("invoiceLine.workOrder"));
        workOrderTitle.getStyleClass().add("row-subtitle");
        Label workOrderValue = new Label();
        workOrderValue.getStyleClass().add("row-title");
        VBox workOrderBox = new VBox(2, workOrderTitle, workOrderValue);

        HBox infoView = new HBox(60, dateBox, workOrderBox);

        TableView<InvoiceLine> lineTable = new TableView<>();

        // Text som visas när fakturan saknar rader (gamla fakturor).
        // Måste sättas före UiKit.styleTable, annars visas standardtexten.
        Label noLinesLabel = UiKit.emptyText("");
        noLinesLabel.textProperty().bind(language.text("invoiceLine.noLines"));
        lineTable.setPlaceholder(noLinesLabel);

        TableColumn<InvoiceLine, String> serviceCol = new TableColumn<>("Service");
        serviceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getServiceName()));

        TableColumn<InvoiceLine, String> finalPriceLineCol = new TableColumn<>("Amount");
        finalPriceLineCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatSek(cellData.getValue().getFinalPrice())));
        finalPriceLineCol.getStyleClass().addAll("cell-right", "cell-strong");

        serviceCol.textProperty().bind(language.text("invoiceLine.service"));
        finalPriceLineCol.textProperty().bind(language.text("invoiceLine.finalPrice"));

        lineTable.getColumns().addAll(serviceCol, finalPriceLineCol);

        UiKit.styleTable(lineTable);

        // Kortet till höger: status, totalbelopp, delsumma och rabatt.
        // Etiketterna skapas tomma här och fylls i när en faktura väljs.

        // Plats för badgen (Betald/Obetald). Badgen byts ut vid varje klick.
        HBox statusHolder = new HBox();

        // "Totalbelopp" med det stora beloppet under
        Label toPayTitle = new Label();
        toPayTitle.textProperty().bind(language.text("invoices.total"));
        toPayTitle.getStyleClass().add("row-subtitle");
        Label toPayValue = new Label();
        toPayValue.getStyleClass().add("stat-value");

        // Raden "Delsumma". Spacern växer och trycker beloppet till höger.
        Label subtotalTitle = new Label();
        subtotalTitle.textProperty().bind(language.text("invoiceLine.sum"));
        subtotalTitle.getStyleClass().add("row-subtitle");
        Label subtotalValue = new Label();
        subtotalValue.getStyleClass().add("row-title");
        Region subtotalSpacer = new Region();
        HBox.setHgrow(subtotalSpacer, Priority.ALWAYS);
        HBox subtotalRow = new HBox(subtotalTitle, subtotalSpacer, subtotalValue);

        // Raden "Rabatt", byggd på samma sätt som Delsumma
        Label discountTitle = new Label();
        discountTitle.textProperty().bind(language.text("invoiceLine.invoiceDiscount"));
        discountTitle.getStyleClass().add("row-subtitle");
        Label discountValue = new Label();
        discountValue.getStyleClass().add("row-title");
        Region discountSpacer = new Region();
        HBox.setHgrow(discountSpacer, Priority.ALWAYS);
        HBox discountRow = new HBox(discountTitle, discountSpacer, discountValue);

        // UiKit.card ger samma ram och bakgrund som andra kort i appen
        VBox summaryCard = UiKit.card(statusHolder, toPayTitle, toPayValue, subtotalRow, discountRow);

        // Rubrik "Utförda tjänster" ovanför radtabellen
        Label linesTitle = new Label();
        linesTitle.textProperty().bind(language.text("invoiceLine.title"));
        linesTitle.getStyleClass().add("row-title");

        // Tabellen till vänster växer, kortet till höger har fast bredd
        VBox linesBox = new VBox(8, linesTitle, lineTable);
        HBox.setHgrow(linesBox, Priority.ALWAYS);
        summaryCard.setPrefWidth(280);
        HBox contentRow = new HBox(16, linesBox, summaryCard);

        // Hela detaljvyn i ett kort: rubrik, infoband och innehåll
        VBox detailCard = UiKit.card(headingLabel, infoView, contentRow);

        // Tipstext som visas tills en faktura väljs
        Label selectHint = UiKit.emptyText("");
        selectHint.textProperty().bind(language.text("invoiceLine.selectInvoice"));

        // Kortet är dolt tills en faktura väljs
        detailCard.setVisible(false);
        detailCard.setManaged(false);

        // När användaren klickar på en faktura visas dess rader i radtabellen.
        // addListener körs automatiskt varje gång valet ändras (Observer-mönstret).
        // newInvoice är null när ingen faktura är vald, därför kontrolleras det först.
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldInvoice, newInvoice) -> {
            if (newInvoice != null) {
                // Visa kortet och dölj tipstexten
                detailCard.setVisible(true);
                detailCard.setManaged(true);
                selectHint.setVisible(false);
                selectHint.setManaged(false);

                ObservableList<InvoiceLine> lines = FXCollections.observableArrayList(newInvoice.getLines());
                lineTable.setItems(lines);

                // Fyller i rubriken och infobandet för den valda fakturan.
                // Rubriken blir t.ex. "Faktura WO-13".
                // workOrderCode gör om arbetsorderns id till "WO-13".
                headingLabel.setText(language.text("invoiceLine.heading").get() + " " + workOrderCode(newInvoice.getWorkOrderId()));
                dateValue.setText(String.valueOf(newInvoice.getInvoiceDate()));
                workOrderValue.setText(workOrderCode(newInvoice.getWorkOrderId()));

                // Fyller i kortet till höger.
                // Badgen visar Betald eller Obetald.
                // Totalbelopp = Delsumma (summan av raderna) minus Rabatt.
                statusHolder.getChildren().setAll(UiKit.statusBadge(newInvoice.isPaid() ? "PAID" : "UNPAID"));
                toPayValue.setText(formatSek(newInvoice.getTotalAmount()));
                subtotalValue.setText(formatSek(newInvoice.getAmount()));
                discountValue.setText("-" + formatSek(newInvoice.getDiscount()));

            }
        });

        VBox view = new VBox(24, UiKit.pageHeader(language.text("invoices.title"), newInvoiceButton), table, selectHint, detailCard);
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
