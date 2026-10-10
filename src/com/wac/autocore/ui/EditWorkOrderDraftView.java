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
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;

public class EditWorkOrderDraftView {

    private final LanguageManager language =
            LanguageManager.getInstance();

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

        DatePicker plannedDateField = new DatePicker();

        plannedDateField.setValue(booking.getDate());

        TextField estimatedMinutesField = new TextField();

        if (booking.getDurationMinutes() > 0) {
            estimatedMinutesField.setText(
                    String.valueOf(booking.getDurationMinutes())
            );
        }

        TextField priceField = new TextField();
        priceField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.pricePrompt")
        );
        estimatedMinutesField.promptTextProperty().bind(
                language.text("editWorkOrderDraft.minutesPrompt")
        );

        TextArea instructionsField = new TextArea();
        instructionsField.setPrefRowCount(3);
        instructionsField.setWrapText(true);

        TextArea commentsField = new TextArea();
        commentsField.setPrefRowCount(3);
        commentsField.setWrapText(true);

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("editWorkOrderDraft.title"), null),
                UiKit.formRow(
                        UiKit.formField(language.text("editWorkOrderDraft.plannedDate"), plannedDateField),
                        UiKit.formField(language.text("editWorkOrderDraft.estimatedMinutes"), estimatedMinutesField)
                ),
                UiKit.formField(language.text("editWorkOrderDraft.price"), priceField),
                UiKit.formField(language.text("editWorkOrderDraft.instructions"), instructionsField),
                UiKit.formField(language.text("editWorkOrderDraft.comments"), commentsField
                )
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;
    }
}

