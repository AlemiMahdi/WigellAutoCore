package com.wac.autocore.ui;


import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;
import com.wac.autocore.service.InvoiceService;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javafx.util.StringConverter;


import java.time.LocalDate;


public class CreateInvoiceView {

    public static VBox build(InvoiceService invoiceService){
        LanguageManager language = LanguageManager.getInstance();


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

        // --- Nya kostnader för reklamationer ---
        TextField extraCostDescriptionField = new TextField();
        extraCostDescriptionField.promptTextProperty().bind(
            language.text("createInvoice.extraCostDescriptionPrompt")
        );

        TextField extraCostAmountField = new TextField();
        extraCostAmountField.promptTextProperty().bind(
            language.text("createInvoice.extraCostAmountPrompt")
        );

        VBox extraCostSection = UiKit.formField(
            language.text("createInvoice.extraCostDescriptionPrompt"),
            extraCostDescriptionField
        );

        VBox extraCostAmountSection = UiKit.formField(
            language.text("createInvoice.extraCostAmountPrompt"),
            extraCostAmountField
        );

        VBox complaintCosts = new VBox(10, extraCostSection, extraCostAmountSection);

        complaintCosts.setVisible(false);
        complaintCosts.setManaged(false);

        // --- Rabatt ---
        // Rabatten anges som en rabattkod (t.ex. WELCOME10 eller SERVICE200)
        TextField discoutField = new TextField();
        discoutField.promptTextProperty().bind(language.text("createInvoice.discountPrompt"));

        // --- Total ---
        Label totalValue = new Label(ShowInvoiceView.formatSek(0));

        Button createButton = UiKit.primaryButton("Create invoice");
        createButton.textProperty().bind(language.text("createInvoice.button"));
        Label statusLabel = UiKit.feedbackLabel();

        // Uppdatera belopp och totalsumma
        Runnable updateTotal = () -> {
            WorkOrder selected = workOrderBox.getValue();

            if (selected == null) {
                amountField.clear();
                totalValue.setText(ShowInvoiceView.formatSek(0));
                return;
            }
            try {
                double extraCost = 0.0;

                if (selected.getType() == WorkOrderTypeEnum.COMPLAINT) {
                    String input = extraCostAmountField.getText().trim();

                    if (!input.isEmpty()) {
                        extraCost = Double.parseDouble(input.replace(',', '.'));
                    }
                }

                Invoice preview = invoiceService.previewInvoice(
                        selected.getId(),
                        discoutField.getText(),
                        extraCostDescriptionField.getText().trim(),
                        extraCost
                );

                if (preview == null) {
                    amountField.clear();
                    totalValue.setText("—");
                    return;
                }

                amountField.setText(
                        ShowInvoiceView.formatSek(preview.getAmount())
                );
                totalValue.setText(
                        ShowInvoiceView.formatSek(preview.getTotalAmount())
                );

            } catch (NumberFormatException | ArithmeticException exception) {
                amountField.clear();
                totalValue.setText("—");
            }
        };

        workOrderBox.valueProperty().addListener((obs, oldValue, selected) -> {
            extraCostDescriptionField.clear();
            extraCostAmountField.clear();
            boolean isComplaint = selected != null
                    && selected.getType() == WorkOrderTypeEnum.COMPLAINT;

            complaintCosts.setVisible(isComplaint);
            complaintCosts.setManaged(isComplaint);
            if (isComplaint) {
                fillComplaintPrice(selected, extraCostAmountField, extraCostDescriptionField, language);
            }
            updateTotal.run();

        });

        extraCostAmountField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateTotal.run()
        );

        discoutField.textProperty().addListener(
                (observable, oldValue, newValue) -> updateTotal.run()
        );

        extraCostDescriptionField.textProperty().addListener(
                (observable, oldValue, newValue) -> updateTotal.run()
        );

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


            double extraCost = 0.0;
            String extraDescription = extraCostDescriptionField.getText().trim();

            if (selectedWorkOrder.getType() == WorkOrderTypeEnum.COMPLAINT) {
                String input = extraCostAmountField.getText().trim();

                if (!input.isEmpty()) {
                    try {
                        extraCost = Double.parseDouble(input.replace(",", "."));

                        if (!Double.isFinite(extraCost) || extraCost < 0) {
                            UiKit.showError(statusLabel,
                                language.text("createInvoice.invalidExtraCost").get());
                            return;
                        }
                    } catch (NumberFormatException exception) {
                        UiKit.showError(statusLabel,
                            language.text("createInvoice.invalidExtraCost").get());
                        return;
                    }

                    if (extraCost > 0 && extraDescription.trim().isEmpty()) {
                        UiKit.showError(statusLabel,
                            language.text("createInvoice.missingExtraDescription").get());
                        return;
                    }
                } else if (!extraDescription.isEmpty()) {
                    UiKit.showError(statusLabel,
                        language.text("createInvoice.invalidExtraCost").get());
                    return;
                }
            }

                try {

                    Invoice invoice = invoiceService.createInvoice(workOrderId, discountCode, extraDescription, extraCost);


                    if (invoice == null) {
                        UiKit.showError(
                                statusLabel,
                                language.text("createInvoice.createError").get()
                        );
                        return;
                    }

                    UiKit.showSuccess(
                            statusLabel,
                            language.text("createInvoice.successFor").get() + " "
                                    + ShowInvoiceView.workOrderCode(workOrderId)
                                    + ". " + language.text("createInvoice.totalLabel").get() + ": "
                                    + ShowInvoiceView.formatSek(invoice.getTotalAmount())
                    );

                    workOrderBox.setValue(null);
                    discoutField.clear();

                } catch (RuntimeException exception) {

                    UiKit.showError(statusLabel,
                            language.text("createInvoice.saveError").get()
                    );
                    exception.printStackTrace();
                }




        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("createInvoice.button"), null),
                UiKit.formField(language.text("createInvoice.workOrderLabel"), workOrderBox),
                UiKit.formField(language.text("invoices.date"), dateField),
                complaintCosts,
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

    private static void fillComplaintPrice(WorkOrder selected,
                                           TextField extraCostAmountField,
                                           TextField extraCostDescriptionField, LanguageManager language) {
        boolean isComplaint = selected != null && selected.getType() == WorkOrderTypeEnum.COMPLAINT;
        assert selected != null;
        if (selected.getEstimatedPrice() != null) {
            extraCostAmountField.setText(selected.getEstimatedPrice().toPlainString());
            extraCostDescriptionField.setText(language.text("createInvoice.getComplaintCost").get());
        } else {
            extraCostDescriptionField.clear();
            extraCostAmountField.clear();
        }
    }

    private static void loadCompletedWorkOrders(ComboBox<WorkOrder> workOrderBox) {
        for (WorkOrder workOrder : Database.getWorkOrders()) {

            boolean alreadyInvoiced = Database.getInvoices()
                    .stream()
                    .anyMatch(invoice ->
                            invoice.getWorkOrderId() == workOrder.getId()
                    );

            if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                    && !alreadyInvoiced) {
                workOrderBox.getItems().add(workOrder);
            }
        }

        if (workOrderBox.getItems().isEmpty()) {
            workOrderBox.promptTextProperty().bind(
                    LanguageManager.getInstance().text("createInvoice.noCompletedPrompt")
            );
        } else {
            workOrderBox.promptTextProperty().bind(
                    LanguageManager.getInstance().text("startWorkOrder.prompt")
            );
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

}
