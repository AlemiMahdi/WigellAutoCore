package com.wac.autocore.ui;

import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Hyperlink;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.WorkOrderRepository;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EditWorkOrderDraftView {

    private final LanguageManager language =
            LanguageManager.getInstance();

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

        Booking booking = Database.getBookings().stream()
                .filter(existing -> existing.getId() == draft.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            javafx.scene.control.Label errorLabel = UiKit.feedbackLabel();
            UiKit.showError(errorLabel, "");
            errorLabel.textProperty().bind(
                    language.text("editWorkOrderDraft.bookingMissing")
            );
            return new VBox(10, errorLabel);
        }

        ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
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

// Förväljer mekanikern om utkastet redan har en.
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

        Set<Integer> selectedServiceIds = new LinkedHashSet<>(draft.getServiceItemIds());

        VBox serviceChoices = new VBox(10);

        for (ServiceItem service : Database.getServiceItems()) {
            CheckBox serviceCheckBox = new CheckBox(service.getName());
            serviceCheckBox.setSelected(selectedServiceIds.contains(service.getId()));

            serviceCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                        if (selected) {
                            selectedServiceIds.add(service.getId());
                        } else {
                            selectedServiceIds.remove(service.getId());
                        }
                    }
            );
            serviceChoices.getChildren().add(serviceCheckBox);
        }

        if (serviceChoices.getChildren().isEmpty()) {
            Label emptyLabel = UiKit.emptyText("");
            emptyLabel.textProperty().bind(
                    language.text("editWorkOrderDraft.emptyService")
            );
            serviceChoices.getChildren().add(emptyLabel);
        }

        DatePicker plannedDateField = new DatePicker();

        plannedDateField.setValue(booking.getDate());

        TextField estimatedMinutesField = new TextField();

        if (booking.getDurationMinutes() > 0) {
            estimatedMinutesField.setText(
                    String.valueOf(booking.getDurationMinutes())
            );
        }

        TextField priceField = new TextField();

        if (draft.getEstimatedPrice() != null) {
            priceField.setText(draft.getEstimatedPrice().toPlainString());
        }

        priceField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.pricePrompt")
        );
        estimatedMinutesField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.minutesPrompt")
        );

        TextArea instructionsField = new TextArea();
        instructionsField.setPrefRowCount(3);
        instructionsField.setWrapText(true);
        instructionsField.setText(
                draft.getCustomerInstructions() == null ? "" : draft.getCustomerInstructions()
        );

        TextArea commentsField = new TextArea();
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

                    if (price.signum() < 0 ||price.compareTo(new BigDecimal("9999999999.99")) > 0){
                        throw new NumberFormatException();
                    }

                } catch (IllegalArgumentException | ArithmeticException exception) {
                    showFeedback (
                        messageLabel, "editWorkOrderDraft.invalidPrice", false);
                        return;


                    }
                }

                java.time.LocalDate plannedDate = plannedDateField.getValue();

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

                    List<Integer> serviceIds = new ArrayList<>(selectedServiceIds);

                    try {
                        new WorkOrderRepository().updateDraft(
                                draft,
                                booking,
                                serviceIds,
                                mechanicId,
                                plannedDate,
                                minutes,
                                price,
                                instructionsField.getText(),
                                commentsField.getText()
                        );

                        showFeedback(messageLabel, "editWorkOrderDraft.saved", true);

                    } catch (IllegalArgumentException exception) {
                        showFeedback(messageLabel, exception.getMessage(), false);

                    } catch (RuntimeException exception) {
                        showFeedback(messageLabel, "editWorkOrderDraft.saveError", false);
                        exception.printStackTrace();
                    }

                });

                VBox form = UiKit.formContainer(
                        UiKit.pageHeader(language.text("editWorkOrderDraft.title"), null),
                        UiKit.formField(language.text("editWorkOrderDraft.mechanic"), mechanicFields),
                        UiKit.formField(language.text("editWorkOrderDraft.services"), serviceChoices
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


