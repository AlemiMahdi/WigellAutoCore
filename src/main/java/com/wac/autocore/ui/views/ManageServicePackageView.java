
package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.security.actions.SecuredServicePackageActions;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ManageServicePackageView {


    private final LanguageManager language = LanguageManager.getInstance();
    private final SecuredServicePackageActions securedServicePackageActions;

    public ManageServicePackageView(
            SecuredServicePackageActions securedServicePackageActions) {
        this.securedServicePackageActions = securedServicePackageActions;
    }

    public VBox getView() {

        ComboBox<ServicePackage> packageBox = new ComboBox<>();
        packageBox.getItems().addAll(securedServicePackageActions.findAll());
        packageBox.promptTextProperty().bind(
            language.text("manageServicePackage.packagePrompt")
        );

        VBox servicesBox = new VBox(8);
        List<CheckBox> checkBoxes = new ArrayList<>();

        for (ServiceItem service : Database.getServiceItems()) {
            CheckBox checkBox = new CheckBox(service.getName());
            checkBox.setUserData(service);
            checkBoxes.add(checkBox);
            servicesBox.getChildren().add(checkBox);
        }

        packageBox.setOnAction(event -> {
            ServicePackage selectedPackage = packageBox.getValue();

            for (CheckBox checkBox : checkBoxes) {
                ServiceItem service =
                        (ServiceItem) checkBox.getUserData();

                boolean selected = selectedPackage != null
                        && selectedPackage.getServices().stream()
                        .anyMatch(existing ->
                                existing.getId() == service.getId());

                checkBox.setSelected(selected);
            }
        });

        Label messageLabel = UiKit.feedbackLabel();
        Button saveButton = UiKit.primaryButton("Save changes");
        saveButton.textProperty().bind(
            language.text("manageServicePackage.saveButton")
        );

        saveButton.setOnAction(event -> {
            ServicePackage selectedPackage = packageBox.getValue();

            if (selectedPackage == null) {
                UiKit.showError(messageLabel,
                        language.text("manageServicePackage.selectPackage").get());
                return;
            }

            List<ServiceItem> selectedServices = new ArrayList<>();

            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isSelected()) {
                    selectedServices.add(
                            (ServiceItem) checkBox.getUserData());
                }
            }
            try {
                securedServicePackageActions.updateServices(selectedPackage, selectedServices);
            } catch (IllegalArgumentException exception) {
                UiKit.showError(messageLabel, language.text(exception.getMessage()).get());
                return;
            } catch (RuntimeException exception) {
                UiKit.showError(messageLabel, language.text("manageServicePackage.saveError").get());
                exception.printStackTrace();
                return;
            }
            UiKit.showSuccess(messageLabel, language.text("manageServicePackage.success").get());

        });

        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("manageServicePackage.title"), null),
                UiKit.formField(language.text("manageServicePackage.packageLabel"), packageBox),
                UiKit.formField(language.text("manageServicePackage.servicesLabel"), servicesBox),
                saveButton,
                messageLabel
        );

        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);

        return view;
    }
}
