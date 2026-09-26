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



public class CreateInvoiceView {

    public static VBox build(){
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

        createButton.setOnAction(actionEvent -> {
            int workOrderId;

            try {
                workOrderId = Integer.parseInt(workOrderIdField.getText());
            } catch(NumberFormatException e) {
                statusLabel.setText("Work order ID has to be a number.");
                return;
            }
            String discountCode = discoutField.getText();


            Invoice invoice = garageSystem.createInvoice(workOrderId, discountCode);

            if (invoice == null) {

                statusLabel.setText(
                        "Could not create invoice. Check work order ID."
                );

            } else {

                try {

                    invoiceRepository.save(invoice);

                    statusLabel.setText(
                            "Invoice created and saved."
                    );

                    workOrderIdField.clear();
                    discoutField.clear();

                } catch (RuntimeException exception) {

                    Database.getInvoices().remove(invoice);
                    statusLabel.setText(
                            "Invoice could not be saved to the database."
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
