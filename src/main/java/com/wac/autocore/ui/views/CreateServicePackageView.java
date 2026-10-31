package com.wac.autocore.ui.views;

import com.wac.autocore.security.actions.SecuredServicePackageActions;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateServicePackageView {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateServicePackageView.class);
    private final LanguageManager language = LanguageManager.getInstance();
    private final SecuredServicePackageActions securedServicePackageActions;

    public CreateServicePackageView(
            SecuredServicePackageActions securedServicePackageActions) {
        this.securedServicePackageActions = securedServicePackageActions;
    }

    public VBox getView() {

        TextField nameField = new TextField();
        nameField.promptTextProperty().bind(
                language.text("createServicePackage.namePrompt")
        );

        Label messageLabel = UiKit.feedbackLabel();
        Button createButton = UiKit.primaryButton("Create package");
        createButton.textProperty().bind(
                language.text("createServicePackage.createButton")
        );


        createButton.setOnAction(event -> {

            String name = nameField.getText();

            try {
                securedServicePackageActions.createServicePackage(name);

                UiKit.showSuccess(messageLabel, language.text("createServicePackage.success").get());

            } catch (IllegalArgumentException exception) {
                UiKit.showError(messageLabel, language.text(exception.getMessage()).get());
                return;
            } catch (RuntimeException exception) {
                UiKit.showError(messageLabel, language.text("createServicePackage.saveError").get());
                LOGGER.error("Could not create service package", exception);
            }

            nameField.clear();
        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("createServicePackage.title"), null),
                UiKit.formField(language.text("createServicePackage.nameLabel"), nameField),
                createButton,
                messageLabel
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;


    }
}
