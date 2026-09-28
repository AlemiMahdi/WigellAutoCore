package com.wac.autocore.ui;


import com.wac.autocore.model.Invoice;
import com.wac.autocore.service.GarageSystem;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.wac.autocore.data.Database;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.ui.language.LanguageManager;



public class CreateInvoiceView {

    public static VBox build(){

        LanguageManager language = LanguageManager.getInstance();

        GarageSystem garageSystem = new GarageSystem();

        InvoiceRepository invoiceRepository = new InvoiceRepository();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        Label workOrderIdLabel = new Label("Work order Id");
        TextField workOrderIdField = new TextField();

        Label discountLabel = new Label("Discount");
        TextField discoutField = new TextField();

        form.add(workOrderIdLabel, 0, 0);
        form.add(workOrderIdField, 1, 0);
        form.add(discountLabel, 0, 1);
        form.add(discoutField, 1, 1);

        Button createButton = new Button("Create invoice");
        Label statusLabel = new Label();

        statusLabel.setWrapText(true);

// Uppdaterar formulärets texter direkt vid språkbyte.
        workOrderIdLabel.textProperty().bind(
                language.text("createInvoice.workOrderId")
        );
        discountLabel.textProperty().bind(language.text("createInvoice.discount"));
        createButton.textProperty().bind(language.text("createInvoice.button"));

        createButton.setOnAction(actionEvent -> {
            int workOrderId;

            try {
                workOrderId = Integer.parseInt(workOrderIdField.getText());
            } catch(NumberFormatException e) {
                statusLabel.textProperty().bind(
                        language.text("createInvoice.invalidId")
                );
                return;
            }
            String discountCode = discoutField.getText();


            Invoice invoice = garageSystem.createInvoice(workOrderId, discountCode);

            if (invoice == null) {

                statusLabel.textProperty().bind(
                        language.text("createInvoice.createError")
                );

            } else {

                try {

                    invoiceRepository.save(invoice);

                    statusLabel.textProperty().bind(
                            language.text("createInvoice.success")
                    );

                    workOrderIdField.clear();
                    discoutField.clear();

                } catch (RuntimeException exception) {

                    Database.getInvoices().remove(invoice);
                    statusLabel.textProperty().bind(
                            language.text("createInvoice.saveError")
                    );
                    exception.printStackTrace();
                }
            }


        });

        VBox root = new VBox(15, form, createButton, statusLabel);
        root.setPadding(new Insets(20));
        return root;


    }
}
