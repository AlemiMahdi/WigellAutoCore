package com.wac.autocore.ui;


import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javafx.util.StringConverter;

import com.wac.autocore.repository.InvoiceRepository;


import java.time.LocalDate;


public class CreateInvoiceView {

    public static VBox build(){
        LanguageManager language = LanguageManager.getInstance();
        GarageSystem garageSystem = new GarageSystem();
        InvoiceRepository invoiceRepository = new InvoiceRepository();
        BookingRepository bookingRepository = new BookingRepository();


        // --- Arbetsorder ---
        // Bara avslutade (COMPLETED) arbetsordrar kan faktureras,
        // så vi visar bara dem i listan.
        ComboBox<WorkOrder> workOrderBox = new ComboBox<>();
        workOrderBox.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder workOrder) {
                return workOrder == null ? "" : describeWorkOrder(workOrder);
            }



            @Override
            public WorkOrder fromString(String text) {
                return null; // Används inte – listan går inte att skriva i
            }
        });
        loadCompletedWorkOrders(workOrderBox);
        UiKit.keepPromptWhenCleared(workOrderBox);

        // --- Datum ---
        // GarageSystem sätter alltid dagens datum på fakturan,
        // så fältet visar bara det och går inte att ändra.
        TextField dateField = new TextField(LocalDate.now().toString());
        dateField.setEditable(false);
        dateField.getStyleClass().add("readonly-field");

        // --- Belopp ---
        // Beloppet räknas ut av GarageSystem från arbetsorderns tjänster.
        // Här visar vi samma summa i förväg, därför är fältet låst.
        TextField amountField = new TextField();
        amountField.setEditable(false);
        amountField.promptTextProperty().bind(language.text("startWorkOrder.prompt"));
        amountField.getStyleClass().add("readonly-field");

        // --- Rabatt ---
        // Rabatten anges som en rabattkod (t.ex. WELCOME10 eller SERVICE200)
        TextField discoutField = new TextField();
        discoutField.promptTextProperty().bind(language.text("createInvoice.discountPrompt"));

        // --- Total ---
        Label totalValue = new Label(ShowInvoiceView.formatSek(0));

        Button createButton = UiKit.primaryButton("Create invoice");
        createButton.textProperty().bind(language.text("createInvoice.button"));
        Label statusLabel = UiKit.feedbackLabel();

        // Uppdatera Amount och Total direkt när man väljer arbetsorder
        workOrderBox.valueProperty().addListener((observable, oldValue, selected) -> {
            if (selected == null) {
                amountField.clear();
                totalValue.setText(ShowInvoiceView.formatSek(0));
            } else {
                Booking booking = bookingRepository.findById(selected.getBookingId());

                double amount = sumServicePrices(selected, booking);
                amountField.setText(ShowInvoiceView.formatSek(amount));
                totalValue.setText(ShowInvoiceView.formatSek(amount));
            }
        });

        // Tala om varför listan är tom, annars ser det ut som ett fel
        if (workOrderBox.getItems().isEmpty()) {
            UiKit.showInfo(statusLabel, language.text("createInvoice.noCompleted").get());
        } else {
            UiKit.showInfo(statusLabel, language.text("createInvoice.discountInfo").get());
        }

        createButton.setOnAction(actionEvent -> {
            WorkOrder selectedWorkOrder = workOrderBox.getValue();

            if (selectedWorkOrder == null) {
                UiKit.showError(statusLabel, language.text("startWorkOrder.select").get());
                return;
            }
            int workOrderId = selectedWorkOrder.getId();
            String discountCode = discoutField.getText();


            Invoice invoice = garageSystem.createInvoice(workOrderId, discountCode);



            if (invoice == null) {

                UiKit.showError(statusLabel,
                        language.text("createInvoice.createError").get()
                );

            } else {

                try {

                    invoiceRepository.save(invoice);

                    UiKit.showSuccess(statusLabel, language.text("createInvoice.successFor").get() + " "
                            + ShowInvoiceView.workOrderCode(workOrderId)
                            + ". " + language.text("createInvoice.totalLabel").get() + ": "
                            + ShowInvoiceView.formatSek(invoice.getTotalAmount())
                    );

                    workOrderBox.setValue(null);
                    discoutField.clear();

                } catch (RuntimeException exception) {

                    Database.getInvoices().remove(invoice);
                    UiKit.showError(statusLabel,
                            language.text("createInvoice.saveError").get()
                    );
                    exception.printStackTrace();
                }

            }


        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("createInvoice.button"), null),
                UiKit.formField(language.text("createInvoice.workOrderLabel"), workOrderBox),
                UiKit.formField(language.text("invoices.date"), dateField),
                UiKit.formRow(
                        UiKit.formField(language.text("invoices.amount"), amountField),
                        UiKit.formField(language.text("invoices.discount"), discoutField)),
                UiKit.totalBox(language.text("createInvoice.totalLabel"), totalValue),
                createButton,
                statusLabel);

        // Formulärkolumnen ska ligga centrerad i innehållsytan
        VBox root = new VBox(form);
        root.setAlignment(Pos.TOP_CENTER);
        return root;


    }

    // Fyller listan med alla arbetsordrar som har status COMPLETED
    private static void loadCompletedWorkOrders(ComboBox<WorkOrder> workOrderBox) {
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            if ("COMPLETED".equals(workOrder.getStatus())) {
                workOrderBox.getItems().add(workOrder);
            }
        }
        if (workOrderBox.getItems().isEmpty()) {
            workOrderBox.promptTextProperty().bind(LanguageManager.getInstance().text("createInvoice.noCompletedPrompt"));
        } else {
            workOrderBox.promptTextProperty().bind(LanguageManager.getInstance().text("startWorkOrder.prompt"));
        }
    }

    // "WO-2 · Volkswagen Passat" – bilen gör det lättare att hitta rätt order
    private static String describeWorkOrder(WorkOrder workOrder) {
        String text = ShowInvoiceView.workOrderCode(workOrder.getId());
        Vehicle vehicle = findVehicleFor(workOrder);
        if (vehicle != null) {
            text += " · " + vehicle.getBrand() + " " + vehicle.getModel();
        }
        return text;
    }

    // Arbetsorder -> bokning -> fordon (null om något saknas)
    private static Vehicle findVehicleFor(WorkOrder workOrder) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == workOrder.getBookingId()) {
                for (Vehicle vehicle : Database.getVehicles()) {
                    if (vehicle.getId() == booking.getVehicleId()) {
                        return vehicle;
                    }
                }
            }
        }
        return null;
    }

    // Samma summa som GarageSystem.createInvoice räknar fram (före rabatt)
    //Lägga till att kolla efter specifika bokningens tjänst
    private static double sumServicePrices(WorkOrder workOrder, Booking booking) {
        double sum = 0.0;

            if(booking != null) {
                for (InvoiceLine frozenLine : booking.getFrozenPrice()){
                    sum+= frozenLine.getPrice();
                }
                return sum;

                }
            else {
                for (Integer serviceItemId : workOrder.getServiceItemIds()) {
                for (ServiceItem serviceItem : Database.getServiceItems()) {
                    if (serviceItem.getId() == serviceItemId) {
                        sum += serviceItem.getPrice();
                    }
                }
            }
        }
        return sum;
    }

}
