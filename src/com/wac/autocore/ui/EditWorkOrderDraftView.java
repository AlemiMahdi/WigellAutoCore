package com.wac.autocore.ui;

import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.ui.views.ViewHelpMethod;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import com.wac.autocore.model.*;
import com.wac.autocore.data.Database;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EditWorkOrderDraftView {

    private final LanguageManager language =
            LanguageManager.getInstance();
    private final WorkOrderService workOrderService;
    private final BookingService bookingService;
    private final ComboBox<WorkOrder> originalCombo = new ComboBox<>();
    private final ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
    private final DatePicker plannedDateField = new DatePicker();
    private final TextField estimatedMinutesField = new TextField();
    private final TextField priceField = new TextField();
    private final TextArea instructionsField = new TextArea();
    private final TextArea commentsField = new TextArea();
    private final Label totalPriceLabel = new Label();

    public EditWorkOrderDraftView(WorkOrderService workOrderService, BookingService bookingService) {
        this.workOrderService = workOrderService;
        this.bookingService = bookingService;
    }

    private void showFeedback(Label label, String key, boolean success) {
        label.textProperty().unbind();

        if (success) {
            UiKit.showSuccess(label, "");
        } else {
            UiKit.showError(label, "");
        }

        label.textProperty().bind(language.text(key));
    }

    public VBox getView(WorkOrder draft) {

        if (draft == null || draft.getStatus() != WorkOrderStatus.DRAFT) {
            javafx.scene.control.Label errorLabel = UiKit.feedbackLabel();
            UiKit.showError(errorLabel, "");
            errorLabel.textProperty().bind(
                    language.text("editWorkOrderDraft.onlyDraft")
            );

            return new VBox(10, errorLabel);
        }

        Booking booking = bookingService.getBookingById(draft.getBookingId()).orElse(null);
        if (booking == null) {
            javafx.scene.control.Label errorLabel = UiKit.feedbackLabel();
            UiKit.showError(errorLabel, "");
            errorLabel.textProperty().bind(
                    language.text("editWorkOrderDraft.bookingMissing")
            );
            return new VBox(10, errorLabel);
        }

        boolean isComplaint = draft.getType() == WorkOrderTypeEnum.COMPLAINT;

        originalCombo.setMaxWidth(Double.MAX_VALUE);
        originalCombo.promptTextProperty().bind(
                language.text("createWorkOrder.selectOriginalWorkOrder")
        );
        List<WorkOrder> completedOrders = workOrderService.findAll().stream()
                .filter(order -> order.getId() != draft.getId())
                .filter(order -> order.getStatus() == WorkOrderStatus.COMPLETED)
                .sorted(Comparator.comparing(WorkOrder::getId).reversed())
                .collect(Collectors.toList());
        originalCombo.getItems().setAll(completedOrders);

        originalCombo.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder order) {
                return order == null ? "" : "WO-" + order.getId();
            }

            @Override
            public WorkOrder fromString(String text) {
                return null;
            }
        });

        for (WorkOrder order : originalCombo.getItems()) {
            if (Integer.valueOf(order.getId()).equals(draft.getOriginalWorkOrderId())) {
                originalCombo.setValue(order);
                break;
            }
        }

        VBox originalField = UiKit.formField(
                language.text("createWorkOrder.originalWorkOrder"),
                originalCombo
        );
        originalField.setVisible(isComplaint);
        originalField.setManaged(isComplaint);

        mechanicCombo.getItems().setAll(Database.getMechanics());
        mechanicCombo.setMaxWidth(Double.MAX_VALUE);
        mechanicCombo.promptTextProperty().bind(
                language.text("editWorkOrderDraft.mechanicPrompt")
        );

        mechanicCombo.setConverter(new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.getName();
            }

            @Override
            public Mechanic fromString(String text) {
                return null;
            }
        });

// väljer mekanikern om utkastet redan har en.
        if (draft.getMechanicId() != 0) {
            for (Mechanic mechanic : mechanicCombo.getItems()) {
                if (mechanic.getId() == draft.getMechanicId()) {
                    mechanicCombo.setValue(mechanic);
                    break;
                }
            }
        }

        Hyperlink clearMechButton = new Hyperlink();
        clearMechButton.textProperty().bind(language.text("editWorkOrderDraft.clearMechanic"));
        clearMechButton.setOnAction(event -> {
            mechanicCombo.getSelectionModel().clearSelection();
        });

        VBox mechanicFields = new VBox(10, mechanicCombo, clearMechButton);

        plannedDateField.setValue(booking.getDate());

        if (booking.getDurationMinutes() > 0) {
            estimatedMinutesField.setText(
                    String.valueOf(booking.getDurationMinutes())
            );
        }
        if (draft.getEstimatedPrice() != null) {
            priceField.setText(draft.getEstimatedPrice().toPlainString());
        }

        priceField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.pricePrompt")
        );
        estimatedMinutesField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.minutesPrompt")
        );

        instructionsField.setPrefRowCount(3);
        instructionsField.setWrapText(true);
        instructionsField.setText(draft.getCustomerInstructions() == null ? "" : draft.getCustomerInstructions());

        commentsField.setPrefRowCount(3);
        commentsField.setWrapText(true);
        commentsField.setText(
                draft.getDraftComments() == null ? "" : draft.getDraftComments()
        );

        Label messageLabel = UiKit.feedbackLabel();
        Button saveButton = UiKit.primaryButton("");
        saveButton.textProperty().bind(
                language.text("editWorkOrderDraft.saveButton")
        );

        ObservableList<ServiceItem> selectedServiceIds = FXCollections.observableArrayList();
        ListView<ServiceItem> serviceList = new ListView<>(
                FXCollections.observableArrayList(Database.getServiceItems())
        );
        serviceList.setPrefHeight(150);

        ViewHelpMethod.setupServiceChecklist(
                serviceList,
                selectedServiceIds,
                estimatedMinutesField,
                totalPriceLabel,
                booking.getServices() // Skickar med existerande tjänster som förval
        );


        saveButton.setOnAction(event -> {
            int minutes;
            BigDecimal price = null;

            String minutesText = estimatedMinutesField.getText().trim();
            String priceText = priceField.getText().trim().replace(" ", "").replace(',', '.');

            try {
                minutes = minutesText.isEmpty() ? 0 : Integer.parseInt(minutesText);

                if (minutes < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                showFeedback(messageLabel, "editWorkOrderDraft.invalidMinutes", false);
                return;
            }

            if (!priceText.isEmpty()) {
                try {
                    price = new BigDecimal(priceText).setScale(2, RoundingMode.UNNECESSARY);

                    if (price.signum() < 0 || price.compareTo(new BigDecimal("9999999999.99")) > 0) {
                        throw new NumberFormatException();
                    }

                } catch (IllegalArgumentException | ArithmeticException exception) {
                    showFeedback(
                            messageLabel, "editWorkOrderDraft.invalidPrice", false);
                    return;


                }
            }

            LocalDate plannedDate = plannedDateField.getValue();

            // Läs även datum som användaren har skrivit utan att lämna fältet.
            try {
                String dateText = plannedDateField.getEditor().getText().trim();
                plannedDate = dateText.isEmpty() ? null : plannedDateField.getConverter().fromString(dateText);

                plannedDateField.setValue(plannedDate);
            } catch (RuntimeException exception) {
                showFeedback(messageLabel, "editWorkOrderDraft.invalidDate", false);
                return;
            }

            Mechanic mechanic = mechanicCombo.getValue();
            int mechanicId = mechanic == null ? 0 : mechanic.getId();

            List<Integer> serviceIds = selectedServiceIds.stream()
                    .map(ServiceItem::getId)
                    .collect(Collectors.toList());

            try {
                workOrderService.updateDraftWorkOrder(draft,
                        booking,
                        serviceIds,
                mechanicId,
                plannedDate,
                minutes,
                price,
                instructionsField.getText(),
                commentsField.getText(),
                        isComplaint && originalCombo.getValue() != null
                                ? originalCombo.getValue().getId()
                                : null);

                showFeedback(messageLabel, "editWorkOrderDraft.saved", true);

            } catch (IllegalArgumentException exception) {
                showFeedback(messageLabel, exception.getMessage(), false);

            } catch (RuntimeException exception) {
                showFeedback(messageLabel, "editWorkOrderDraft.saveError", false);
                exception.printStackTrace();
            }

        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("editWorkOrderDraft.title"), null ),originalField,
                UiKit.formField(language.text("editWorkOrderDraft.mechanic"), mechanicFields),
                UiKit.formField(language.text("editWorkOrderDraft.services"), serviceList
                ),
                UiKit.formRow(
                        UiKit.formField(language.text("editWorkOrderDraft.plannedDate"), plannedDateField),
                        UiKit.formField(language.text("editWorkOrderDraft.estimatedMinutes"), estimatedMinutesField)
                ),
                UiKit.formField(language.text("editWorkOrderDraft.price"), priceField),
                UiKit.formField(language.text("editWorkOrderDraft.instructions"), instructionsField),
                UiKit.formField(language.text("editWorkOrderDraft.comments"), commentsField), saveButton, messageLabel

        );

        clearMechButton.setStyle(
                "-fx-text-fill: -fx-text-muted; -fx-padding: 2 0;"
        );

        clearMechButton.disableProperty().bind(
                mechanicCombo.valueProperty().isNull()
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;
    }
}


