package com.wac.autocore.ui;

import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class EditWorkOrderDraftView {

    private final LanguageManager language =
            LanguageManager.getInstance();

    public VBox getView() {
        TextArea instructionsField = new TextArea();
        instructionsField.setPrefRowCount(3);
        instructionsField.setWrapText(true);

        TextArea commentsField = new TextArea();
        commentsField.setPrefRowCount(3);
        commentsField.setWrapText(true);

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("editWorkOrderDraft.title"), null),
                UiKit.formField(language.text("editWorkOrderDraft.instructions"), instructionsField),
                UiKit.formField(language.text("editWorkOrderDraft.comments"), commentsField
                )
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;
    }
}

