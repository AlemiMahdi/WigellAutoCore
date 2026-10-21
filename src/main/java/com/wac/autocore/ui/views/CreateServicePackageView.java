package com.wac.autocore.ui.views;

import com.wac.autocore.service.ServicePackageService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateServicePackageView {

    private final LanguageManager language = LanguageManager.getInstance();
    private final ServicePackageService servicePackageService;

    public CreateServicePackageView(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
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
                servicePackageService.createServicePackage(name);

                UiKit.showSuccess(messageLabel, language.text("createServicePackage.success").get());

            } catch (IllegalArgumentException exception) {
                UiKit.showError(messageLabel, language.text(exception.getMessage()).get());
                return;
            } catch (RuntimeException exception) {
                UiKit.showError(messageLabel, language.text("createServicePackage.saveError").get());
                exception.printStackTrace();
                return;
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
