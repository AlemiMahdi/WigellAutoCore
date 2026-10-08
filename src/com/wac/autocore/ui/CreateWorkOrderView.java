package com.wac.autocore.ui;

import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.workOrderType.WorkOrderTypeEnum;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Node;


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
        UiKit.keepPromptWhenCleared(typeComboBox);

        //ny för drop-in arbetsorder
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getVehicles()));
        vehicleComboBox.promptTextProperty().bind(language.text("createWorkOrder.selectVehicle"));
        vehicleComboBox.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleComboBox);

        ListView<ServiceItem> serviceListView = new ListView<>(FXCollections.observableArrayList(Database.getServiceItems()));
        serviceListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serviceListView.setPrefHeight(120); // Ger listan en lagom höjd i formuläret
        serviceListView.setCellFactory(param -> new ListCell<ServiceItem>() {
            @Override
            protected void updateItem(ServiceItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getPrice() + " kr)");
                }
            }
        });

        VBox typeField = UiKit.formField(language.text("createWorkOrder.type"), typeComboBox);
        VBox vehicleField = UiKit.formField(language.text("createWorkOrder.vehicle"), vehicleComboBox);
        VBox bookingField = UiKit.formField(language.text("createWorkOrder.bookingLabel"), bookingComboBox);
        VBox mechanicField = UiKit.formField(language.text("bookings.mechanic"), mechanicComboBox);
        VBox serviceField = UiKit.formField(language.text("createWorkOrder.service"), serviceListView);

        vehicleField.setVisible(false);
        vehicleField.setManaged(false);
        serviceField.setVisible(false);
        serviceField.setManaged(false);

        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isDropIn = (newValue == WorkOrderTypeEnum.DROP_IN);

            vehicleField.setVisible(isDropIn);
            vehicleField.setManaged(isDropIn);
            serviceField.setVisible(isDropIn);
            serviceField.setManaged(isDropIn);
            // Göm bokningsfältet om det är Drop-in
            bookingField.setVisible(!isDropIn);
            bookingField.setManaged(!isDropIn);
        });

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

            WorkOrderDto dto = new WorkOrderDto();
            dto.setMechanicId(mechanic.getId());

            // Sparar bokningens status ifall vi behöver återställa den.
            String previousBookingStatus = booking.getStatus();

            if(selectedType == WorkOrderTypeEnum.DROP_IN){
                Vehicle vehicle = vehicleComboBox.getValue();
                List<ServiceItem> selectedService = serviceListView.getSelectionModel().getSelectedItems();

                if(vehicle == null || selectedService.isEmpty()){
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.noSelection").get());
                    return;
                }
                dto.setVehicleId(vehicle.getId());
                List<Integer> serviceIds = new ArrayList<>();
                for(ServiceItem item : selectedService){
                    serviceIds.add(item.getId());
                }
                dto.setServices(serviceIds);
            } else {
                // Gäller PLANNED och COMPLAINT
                booking = bookingComboBox.getValue();
                if (booking == null) {
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.missingSelection").get());
                    return;
                }
                dto.setBookingId(booking.getId());
                previousBookingStatus = booking.getStatus();
            }

            //GarageSystem anropar WorkOrderTypeManager
            WorkOrder workOrder = garageSystem.createWorkOrder(dto, selectedType);

            if(workOrder == null){
                if (!mechanic.isAvailable()){
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.unavailable").get());
                }
                else {
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.createError").get());
                }
                return;
            }

            try {
                // Sparar arbetsordern och bokningens nya status i MySQL.
                workOrderRepository.save(workOrder);
                if(booking != null) {
                    bookingRepository.save(booking);

                } else {
                    Booking booked = Database.getBookings().get(booking.getId());
                    bookingRepository.save(booked);
                }
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
            vehicleComboBox.getSelectionModel().clearSelection();
            vehicleComboBox.setValue(null);
            serviceListView.getSelectionModel().clearSelection();

        });

        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader(language.text("createWorkOrder.title"), null),
                UiKit.formField(language.text("createWorkOrder.type"), typeComboBox),
                UiKit.formField(language.text("createWorkOrder.vehicle"), vehicleComboBox),
                UiKit.formField(language.text("createWorkOrder.bookingLabel"), bookingComboBox),
                UiKit.formField(language.text("bookings.mechanic"), mechanicComboBox),
                UiKit.formField(language.text("createWorkOrder.service"), serviceListView),
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

    private StringConverter vehicleConverter() {
        return new StringConverter() {
            @Override
            public String toString(Vehicle vehicle) {
                if (vehicle == null) return "";
                return vehicle.getRegistrationNumber() + " (" + vehicle.getModel() + ")";
            }
            @Override public Vehicle fromString(String text) { return null; }
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
