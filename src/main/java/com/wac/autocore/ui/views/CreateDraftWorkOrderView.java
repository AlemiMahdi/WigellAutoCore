package com.wac.autocore.ui.views;

import com.wac.autocore.ui.ErrorHandler;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderTypeEnum;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

// Formulär för att skapa en arbetsorder som utkast (WAC-63)
// Bara fordon, typ och en kort problembeskrivning krävs.
public class CreateDraftWorkOrderView {

    private final LanguageManager language = LanguageManager.getInstance();
    private final WorkOrderService workOrderService;

    public CreateDraftWorkOrderView(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    public VBox getView() {

        // 1. Fälten
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>(
                FXCollections.observableArrayList(Database.getVehicles())
        );
        vehicleComboBox.promptTextProperty().bind(language.text("createDraft.vehiclePrompt"));
        vehicleComboBox.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleComboBox);

        ComboBox<WorkOrderTypeEnum> typeCombo = new ComboBox<>(
                FXCollections.observableArrayList(WorkOrderTypeEnum.values())
        );
        typeCombo.promptTextProperty().bind(language.text("createDraft.typePrompt"));
        typeCombo.setConverter(typeConverter());
        UiKit.keepPromptWhenCleared(typeCombo);

        ComboBox<WorkOrder> originalWorkOrderCombo = new ComboBox<>();
        originalWorkOrderCombo.setMaxWidth(Double.MAX_VALUE);
        originalWorkOrderCombo.promptTextProperty().bind(
                language.text("createWorkOrder.selectOriginalWorkOrder")
        );

        // Fyll rullistan med alla avslutade (COMPLETED) arbetsordrar från minnet
        for (WorkOrder order : UiKit.newestFirst(
            Database.getWorkOrders(), 
            WorkOrder::getId
        )) {
            if (order.getStatus() == WorkOrderStatus.COMPLETED) {
                originalWorkOrderCombo.getItems().add(order);
            }
        }

        // Formatera texten i rullistan till "WO-ID" (t.ex. WO-36)
        originalWorkOrderCombo.setConverter(new StringConverter<WorkOrder>() {
            @Override
            public String toString(WorkOrder order) {
                return order == null ? "" : "WO-" + order.getId();
            }

            @Override
            public WorkOrder fromString(String text) {
                return null;
            }
        });

        VBox originalWorkOrderField = UiKit.formField(
                language.text("createWorkOrder.originalWorkOrder"),
                originalWorkOrderCombo
        );

        // Dölj fältet som standard vid start
        originalWorkOrderField.setVisible(false);
        originalWorkOrderField.setManaged(false);

        typeCombo.valueProperty().addListener((obs, oldValue, selectedType) -> {
            boolean isComplaint = selectedType == WorkOrderTypeEnum.COMPLAINT;
            originalWorkOrderField.setVisible(isComplaint);
            originalWorkOrderField.setManaged(isComplaint);
            if (!isComplaint) {
                originalWorkOrderCombo.getSelectionModel().clearSelection();
            }
        });

        TextArea descriptionField = new TextArea();
        descriptionField.promptTextProperty().bind(language.text("createDraft.descriptionPrompt"));
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);


        Label messageLabel = UiKit.feedbackLabel();

        Button createButton = UiKit.primaryButton("");
        createButton.textProperty().bind(language.text("createDraft.button"));

        //2. Vad som händer när man klickar
        createButton.setOnAction(actionEvent -> {

            //Kontollera att något är valt i listorna
            Vehicle vehicle = vehicleComboBox.getValue();
            if (vehicle == null) {
                UiKit.showError(messageLabel, language.text("createDraft.vehicleMissing").get());
                return;
            }

            WorkOrderTypeEnum type = typeCombo.getValue();
            if (type == null) {
                UiKit.showError(messageLabel, language.text("createDraft.typeMissing").get());
                return;
            }

            Integer originalId = null;
            if (type == WorkOrderTypeEnum.COMPLAINT) {
                WorkOrder originalOrder = originalWorkOrderCombo.getValue();
                if (originalOrder == null) {
                    // Visar felmeddelande om man glömt att välja vilken order som reklameras
                    UiKit.showError(messageLabel, language.text("createDraft.originalMissing").get());
                    return;
                }
                originalId = originalOrder.getId();
            }
            // WorkOrderService kontrollerar reglerna, sparar i MySQL och lägger i minnet.
            try {
                workOrderService.createNewDraftWorkOrder(
                        vehicle.getId(), descriptionField.getText(), type, originalId);
            } catch (IllegalArgumentException exception) {
                // Felets text är en språknyckel, t.ex. "createDraft.descriptionMissing"
                UiKit.showError(messageLabel, language.text(exception.getMessage()).get());
                return;
            } catch (RuntimeException exception) {
                ErrorHandler.show(messageLabel, exception, "createDraft.saveError",
                        "vehicle " + vehicle.getId() + ", type " + type);
                return;
            }

            UiKit.showSuccess(messageLabel, language.text("createDraft.success").get());

            //Tömmer formuläret så att nästa utkast kan skriva i direkt
            vehicleComboBox.setValue(null);
            typeCombo.setValue(null);
            descriptionField.clear();
        });

        //3. Layout
        VBox form = UiKit.formContainer(
                UiKit.pageHeader(language.text("createDraft.title"),null),
                UiKit.formField(language.text("createDraft.vehicle"), vehicleComboBox ),
                UiKit.formField(language.text("createDraft.type"), typeCombo ),
                originalWorkOrderField,
                UiKit.formField(language.text("createDraft.description"), descriptionField ),
                createButton,
                messageLabel
        );
        VBox view = new VBox(form);
        view.setAlignment(Pos.TOP_CENTER);
        return view;

    }

    //Visar fordon som "ABC123 · Volvo V70"
    private StringConverter<Vehicle> vehicleConverter() {
        return new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                if(vehicle == null) {
                    return "";
                }
                return  vehicle.getRegistrationNumber() + " · "
                        + vehicle.getBrand() + " " + vehicle.getModel();
            }

            @Override
            public Vehicle fromString(String s) {
                return null;
            }
        };
    }

    //Visar typen med text från språkfilen, t.ex PLANNED -> "planerad"
    private StringConverter<WorkOrderTypeEnum> typeConverter() {
        return new StringConverter<WorkOrderTypeEnum>() {
            @Override
            public String toString(WorkOrderTypeEnum type) {
                if(type == null) {
                    return "";
                }
                return language.text("createDraft.type." + type.name()).get();
            }

            @Override
            public WorkOrderTypeEnum fromString(String s) {
                return null;
            }
        };
    }

}
