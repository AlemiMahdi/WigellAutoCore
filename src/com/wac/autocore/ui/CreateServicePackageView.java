package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.ServicePackageRepository;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateServicePackageView {

     private final LanguageManager language = LanguageManager.getInstance();
    
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

        ServicePackageRepository repository = new ServicePackageRepository();

        createButton.setOnAction( event -> {
            
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                UiKit.showError(messageLabel, language.text("createServicePackage.nameRequired").get());
                return;
            }

            ServicePackage servicePackage = new ServicePackage();
            servicePackage.setName(name);

            try {
                repository.save(servicePackage);
                Database.getServicePackages().add(servicePackage);
                UiKit.showSuccess(messageLabel, language.text("createServicePackage.success").get());

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
