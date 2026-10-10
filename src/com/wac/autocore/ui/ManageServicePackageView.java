
package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.repository.ServicePackageRepository;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class ManageServicePackageView {

    private final ServicePackageRepository repository =
            new ServicePackageRepository();
    private final LanguageManager language = LanguageManager.getInstance();

    public VBox getView() {

        ComboBox<ServicePackage> packageBox = new ComboBox<>();
        packageBox.getItems().addAll(Database.getServicePackages());
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

            List<ServiceItem> previousServices =
                    new ArrayList<>(selectedPackage.getServices());

            selectedPackage.setServices(selectedServices);

            try {
                repository.save(selectedPackage);
                UiKit.showSuccess(messageLabel,
                        language.text("manageServicePackage.success").get());
            } catch (RuntimeException exception) {
                selectedPackage.setServices(previousServices);
                UiKit.showError(messageLabel,
                        language.text("manageServicePackage.saveError").get());
                exception.printStackTrace();
            }
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
