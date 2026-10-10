package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;


//Formulär för att skapa arbetsordrar.
public class CreateWorkOrderView  extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();

    public CreateWorkOrderView() {
        // Centrerar formulärkolumnen i innehållsytan
        setAlignment(Pos.TOP_CENTER);

        ComboBox<Booking> bookingComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getBookings()));
        bookingComboBox.promptTextProperty().bind(language.text("createWorkOrder.selectBooking"));
        bookingComboBox.setConverter(bookingConverter());
        UiKit.keepPromptWhenCleared(bookingComboBox);

        ComboBox<Mechanic> mechanicComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getMechanics()));
        mechanicComboBox.promptTextProperty().bind(language.text("createWorkOrder.selectMechanic"));
        mechanicComboBox.setConverter(mechanicConverter());
        UiKit.keepPromptWhenCleared(mechanicComboBox);

        ComboBox<WorkOrderTypeEnum> typeComboBox = new ComboBox<>(FXCollections.observableArrayList(WorkOrderTypeEnum.values()));
        typeComboBox.setConverter(typeConverter());
        typeComboBox.setValue(WorkOrderTypeEnum.PLANNED);

        ComboBox<WorkOrder> originalWorkOrderCombo = new ComboBox<>(
            FXCollections.observableArrayList(
                Database.getWorkOrders()
                    .stream()
                    .filter(workOrder ->
                        workOrder.getStatus() == WorkOrderStatus.COMPLETED
                    )
                    .collect(java.util.stream.Collectors.toList())
            )
        );
        originalWorkOrderCombo.promptTextProperty().bind(
                language.text("createWorkOrder.selectOriginalWorkOrder")
        );
        originalWorkOrderCombo.setConverter(new StringConverter<WorkOrder>() {

            
            @Override
            public String toString(WorkOrder workOrder) {
                if (workOrder == null) {
                    return "";
                }
                return "#" + workOrder.getId()
                + " - Booking ID: " + workOrder.getBookingId();
            }

            @Override
            public WorkOrder fromString(String text) {
                return null;
            }

            
        });

        UiKit.keepPromptWhenCleared(originalWorkOrderCombo);
        VBox originalWorkOrderField = UiKit.formField(
                language.text("createWorkOrder.originalWorkOrder"),
                originalWorkOrderCombo
        );
        originalWorkOrderField.visibleProperty().bind(
            typeComboBox.valueProperty().isEqualTo(WorkOrderTypeEnum.COMPLAINT)
        );
        originalWorkOrderField.managedProperty().bind(
            originalWorkOrderField.visibleProperty()
        );

        // Välj automatiskt originalarbetsorderns bokning vid reklamation.
        originalWorkOrderCombo.valueProperty().addListener(
            (obs, oldValue, originalWorkOrder) -> {

                if (typeComboBox.getValue() != WorkOrderTypeEnum.COMPLAINT) {
                    return;
                }

                if (originalWorkOrder == null) {
                    bookingComboBox.setValue(null);
                    return;
                }

                Booking matchingBooking = bookingComboBox.getItems()
                    .stream()
                    .filter(booking ->
                        booking.getId() == originalWorkOrder.getBookingId()
                    )
                    .findFirst()
                    .orElse(null);

                bookingComboBox.setValue(matchingBooking);
            }
        );

        // Lås bokningsfältet för reklamationer.
        bookingComboBox.disableProperty().bind(
            typeComboBox.valueProperty().isEqualTo(WorkOrderTypeEnum.COMPLAINT)
        );

        // Återställ bokningsvalet när arbetsordertypen ändras.
        typeComboBox.valueProperty().addListener(
            (obs, oldType, newType) -> {

                if (newType == WorkOrderTypeEnum.COMPLAINT) {
                    originalWorkOrderCombo.setValue(null);
                    bookingComboBox.setValue(null);
                } else if (oldType == WorkOrderTypeEnum.COMPLAINT) {
                    originalWorkOrderCombo.setValue(null);
                    bookingComboBox.setValue(null);
                }
            }
        );

        Button saveButton = UiKit.primaryButton("Create work order");
        saveButton.textProperty().bind(language.text("createWorkOrder.title"));

        Label feedbackLabel = UiKit.feedbackLabel();

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();

        saveButton.setOnAction(event -> {
            Booking booking = bookingComboBox.getValue();
            Mechanic mechanic = mechanicComboBox.getValue();
            WorkOrderTypeEnum selectedType = typeComboBox.getValue();

            if (booking == null || mechanic == null || selectedType == null) {
                UiKit.showError(feedbackLabel, language.text("createWorkOrder.missingSelection").get());

                return;
            }

            WorkOrder originalWorkOrder = originalWorkOrderCombo.getValue();
            if (selectedType == WorkOrderTypeEnum.COMPLAINT && originalWorkOrder == null) {
                UiKit.showError(feedbackLabel, language.text("createWorkOrder.missingOriginalWorkOrder").get());
                return;
            }

            // Sparar bokningens status ifall vi behöver återställa den.
            String previousBookingStatus = booking.getStatus();

            //GarageSystem avgör om arbetsordern får skapas eller inte.
            WorkOrder workOrder = garageSystem.createWorkOrder(booking.getId(), mechanic.getId(), selectedType);

            if(workOrder == null){
                if (!mechanic.isAvailable()){
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.unavailable").get());
                }
                else {
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.createError").get());
                }
                return;
            }
            if (selectedType == WorkOrderTypeEnum.COMPLAINT) {
                workOrder.setOriginalWorkOrderId(originalWorkOrder.getId());
            }

            try {
                // Sparar arbetsordern och bokningens nya status i MySQL.
                workOrderRepository.save(workOrder);
                bookingRepository.save(booking);
            } catch (RuntimeException exception) {
                // Tar bort arbetsordern ur minnet och återställer bokningen
                // om databassparandet misslyckas.
                Database.getWorkOrders().remove(workOrder);
                booking.setStatus(previousBookingStatus);

                UiKit.showError(feedbackLabel, language.text("createWorkOrder.saveError").get());
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(feedbackLabel, language.text("createWorkOrder.success").get());

            //Jag tömmer valen efter registreringen har lyckats.
            bookingComboBox.getSelectionModel().clearSelection();
            bookingComboBox.setValue(null);
            mechanicComboBox.getSelectionModel().clearSelection();
            mechanicComboBox.setValue(null);
            typeComboBox.setValue(WorkOrderTypeEnum.PLANNED);
            originalWorkOrderCombo.getSelectionModel().clearSelection();
            originalWorkOrderCombo.setValue(null);

        });

        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader(language.text("createWorkOrder.title"), null),
                UiKit.formField(language.text("createWorkOrder.type"), typeComboBox),
                originalWorkOrderField,
                UiKit.formField(language.text("createWorkOrder.bookingLabel"), bookingComboBox),
                UiKit.formField(language.text("bookings.mechanic"), mechanicComboBox),
                saveButton,
                feedbackLabel
        ));
    }

    // Visar bokningen som "#3 · GHI321 · Tire change"
    private StringConverter<Booking> bookingConverter() {
        return new StringConverter<Booking>() {
            @Override
            public String toString(Booking booking) {
                if (booking == null) {
                    return "";
                }
                return "#" + booking.getId() + " · "
                        + registrationNumber(booking.getVehicleId()) + " · "
                        + booking.getDescription();
            }

            @Override
            public Booking fromString(String text) {
                // Används inte – combo-boxen går inte att skriva i
                return null;
            }
        };
    }

    private StringConverter<Mechanic> mechanicConverter() {
        return new StringConverter<Mechanic>() {
            @Override
            public String toString(Mechanic mechanic) {
                return mechanic == null ? "" : mechanic.getName();
            }

            @Override
            public Mechanic fromString(String text) {
                return null;
            }
        };
    }

    private StringConverter<WorkOrderTypeEnum> typeConverter() {
        return new StringConverter<WorkOrderTypeEnum>() {
            @Override
            public String toString(WorkOrderTypeEnum type) {
                if (type == null) return "";
                    switch(type) {
                        case PLANNED: return language.text("workOrder.type.planned").get();
                        case DROP_IN: return language.text("workOrder.type.dropIn").get();
                        case COMPLAINT: return language.text("workOrder.type.complaint").get();
                        default: return type.name().toLowerCase();
                    }
            }
            @Override
            public WorkOrderTypeEnum fromString(String string) {
                return null;
            }
        };
    }

    private String registrationNumber(int vehicleId) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == vehicleId) {
                return vehicle.getRegistrationNumber();
            }
        }
        return language.text("bookings.vehicleId").get() + " " + vehicleId;
    }
}
