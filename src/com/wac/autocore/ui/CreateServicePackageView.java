package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.ServicePackageRepository;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateServicePackageView {
    
    public VBox getView() {

        TextField nameField = new TextField();
        nameField.setPromptText("Package name");

        Label messageLabel = UiKit.feedbackLabel();
        Button createButton = UiKit.primaryButton("Create package");

        ServicePackageRepository repository = new ServicePackageRepository();

        createButton.setOnAction( event -> {
            
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                UiKit.showError(messageLabel, "Package name is required");
                return;
            }

            ServicePackage servicePackage = new ServicePackage();
            servicePackage.setName(name);

            try {
                repository.save(servicePackage);
                Database.getServicePackages().add(servicePackage);
                UiKit.showSuccess(messageLabel, "Service package created successfully");

            } catch (RuntimeException exception) {
                UiKit.showError(messageLabel, "Could not save service package");
                exception.printStackTrace();
                return;
            }
            nameField.clear();
        });

        VBox form = UiKit.formContainer(
            UiKit.pageHeader("Create service package", null),
            UiKit.formField("Package name", nameField),
            createButton,
            messageLabel
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;
        
        
    }
}
