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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        StringConverter<WorkOrderTypeEnum> typeConverter = new StringConverter<WorkOrderTypeEnum>() {
            @Override
            public String toString(WorkOrderTypeEnum type) {
                if (type == null) return "";
                switch (type) {
                    case PLANNED: return language.text("workOrder.type.planned").get();
                    case DROP_IN: return language.text("workOrder.type.dropIn").get();
                    case COMPLAINT: return language.text("workOrder.type.complaint").get();
                    default: return type.name();
                }
            }

            @Override
            public WorkOrderTypeEnum fromString(String string) {
                return null;
            }
        };

        typeComboBox.setConverter(typeConverter);
        typeComboBox.setCellFactory(param -> new ListCell<WorkOrderTypeEnum>() {
            //visar när listan är öppen för rätt språk
            @Override
            protected void updateItem(WorkOrderTypeEnum type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    textProperty().unbind();
                    setText(null);
                } else {
                    switch (type) {
                        case PLANNED: textProperty().bind(language.text("workOrder.type.planned")); break;
                        case DROP_IN: textProperty().bind(language.text("workOrder.type.dropIn")); break;
                        case COMPLAINT: textProperty().bind(language.text("workOrder.type.complaint")); break;
                        default: textProperty().unbind(); setText(type.name());
                    }
                }
            }
        });

        ComboBox<Vehicle> vehicleCombo = new ComboBox<>(FXCollections.observableArrayList(Database.getVehicles()));
        vehicleCombo.promptTextProperty().bind(language.text("createBooking.vehiclePrompt"));
        vehicleCombo.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleCombo);

        DatePicker datePicker = new DatePicker();
        datePicker.promptTextProperty().bind(language.text("createBooking.datePrompt"));

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");

        TextField durationField = new TextField();
        durationField.promptTextProperty().bind(language.text("createBooking.durationPrompt"));
        durationField.setEditable(false);
        durationField.setText("0");

        Label totalPriceLabel = new Label();

        ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();

        // Automatisk beräkning av tid och pris
        Runnable updateTotals = () -> {
            int totalMinutes = selectedServices.stream().mapToInt(ServiceItem::getEstimatedMinutes).sum();
            double totalPrice = selectedServices.stream().mapToDouble(ServiceItem::getPrice).sum();

            durationField.setText(String.valueOf(totalMinutes));
            totalPriceLabel.setText(String.format(Locale.ROOT, "%.2f", totalPrice));
        };

        selectedServices.addListener((ListChangeListener<ServiceItem>) change -> updateTotals.run());
        updateTotals.run();

        ListView<ServiceItem> serviceList = new ListView<>(FXCollections.observableArrayList(Database.getServiceItems()));
        serviceList.setPrefHeight(150);
        serviceList.setCellFactory(listView -> new ListCell<ServiceItem>() {
            private final CheckBox checkBox = new CheckBox();
            @Override
            protected void updateItem(ServiceItem service, boolean empty) {
                super.updateItem(service, empty);
                if (empty || service == null) {
                    setGraphic(null);
                    return;
                }
                checkBox.setText(service.getName() + " – " + service.getPrice() + " kr – " + service.getEstimatedMinutes() + " min");
                checkBox.setSelected(selectedServices.contains(service));
                checkBox.setOnAction(event -> {
                    if (checkBox.isSelected()) {
                        if (!selectedServices.contains(service)) selectedServices.add(service);
                    } else {
                        selectedServices.remove(service);
                    }
                });
                setGraphic(checkBox);
            }
        });

        //styr värdet när listan är stängd, så språket inte blir konstigt i listan
        typeComboBox.setButtonCell(new ListCell<WorkOrderTypeEnum>() {
            @Override
            protected void updateItem(WorkOrderTypeEnum type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    textProperty().unbind();
                    setText(null);
                } else {
                    switch (type) {
                        case PLANNED: textProperty().bind(language.text("workOrder.type.planned")); break;
                        case DROP_IN: textProperty().bind(language.text("workOrder.type.dropIn")); break;
                        case COMPLAINT: textProperty().bind(language.text("workOrder.type.complaint")); break;
                        default: textProperty().unbind(); setText(type.name());
                    }
                }
            }
        });

        typeComboBox.setValue(WorkOrderTypeEnum.PLANNED);
        UiKit.keepPromptWhenCleared(typeComboBox);

        //ny för drop-in arbetsorder
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getVehicles()));
        vehicleComboBox.promptTextProperty().bind(language.text("createWorkOrder.selectVehicle"));
        vehicleComboBox.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleComboBox);

        Button CreateCustomerButton = UiKit.primaryButton("+ Customer");
        CreateCustomerButton.setTooltip(new Tooltip("Snabbregistrera ny kund"));
        CreateCustomerButton.setOnAction(event -> Navigator.goTo("create-customer")); // Anpassa till din söksträng i Navigator

        Button CreateVehicleButton = UiKit.primaryButton("+ Vehicle");
        CreateVehicleButton.setTooltip(new Tooltip("Snabbregistrera nytt fordon på kund"));
        CreateVehicleButton.setOnAction(event -> Navigator.goTo("create-vehicle")); // Anpassa till din söksträng i Navigator
        HBox vehicleRowContainer = UiKit.formRow(vehicleComboBox, CreateCustomerButton, CreateVehicleButton);

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
        VBox vehicleField = UiKit.formField(language.text("createWorkOrder.vehicle"), vehicleRowContainer);
        VBox bookingField = UiKit.formField(language.text("createWorkOrder.bookingLabel"), bookingComboBox);
        VBox mechanicField = UiKit.formField(language.text("bookings.mechanic"), mechanicComboBox);
        VBox serviceField = UiKit.formField(language.text("createWorkOrder.service"), serviceListView);
        HBox dateTimeRow = UiKit.formRow(
                UiKit.formField(language.text("bookings.date"), datePicker),
                UiKit.formField(language.text("bookings.startTime"), startTimeField)
        );
        HBox durationMechanicRow = UiKit.formRow(
                UiKit.formField(language.text("createBooking.durationLabel"), durationField),
                mechanicField
        );
        VBox priceField = UiKit.formField(language.text("createBooking.totalPrice"), totalPriceLabel);

        CreateCustomerButton.textProperty().bind(language.text("createWorkOrder.buttonCustomer"));
        CreateVehicleButton.textProperty().bind(language.text("createWorkOrder.buttonVehicle"));

        vehicleField.setVisible(false);
        vehicleField.setManaged(false);
        serviceField.setVisible(false);
        serviceField.setManaged(false);
        dateTimeRow.setVisible(false); dateTimeRow.setManaged(false);
        priceField.setVisible(false); priceField.setManaged(false);

        Label feedbackLabel = UiKit.feedbackLabel();

        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            boolean isDropIn = (newValue == WorkOrderTypeEnum.DROP_IN);
        // Rensa felmeddelande vid byte av typ
            feedbackLabel.setText("");

            vehicleField.setVisible(isDropIn);
            vehicleField.setManaged(isDropIn);
            serviceField.setVisible(isDropIn);
            serviceField.setManaged(isDropIn);
            // Göm bokningsfältet om det är Drop-in
            bookingField.setVisible(!isDropIn);
            bookingField.setManaged(!isDropIn);
            dateTimeRow.setVisible(isDropIn); dateTimeRow.setManaged(isDropIn);
            priceField.setVisible(isDropIn); priceField.setManaged(isDropIn);
            durationMechanicRow.getChildren().clear();
            if (isDropIn) {
                durationMechanicRow.getChildren().addAll(
                        UiKit.formField(language.text("createBooking.durationLabel"), durationField),
                        mechanicField
                );
            }
        });

        Button saveButton = UiKit.primaryButton("Create work order");
        saveButton.textProperty().bind(language.text("createWorkOrder.title"));

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();

        saveButton.setOnAction(event -> {
            Mechanic mechanic = mechanicComboBox.getValue();
            WorkOrderTypeEnum selectedType = typeComboBox.getValue();

            if (mechanic == null || selectedType == null) {
                UiKit.showError(feedbackLabel, language.text("createWorkOrder.missingSelection").get());

                return;
            }

            WorkOrderDto dto = new WorkOrderDto();
            dto.setMechanicId(mechanic.getId());

            Booking booking = null;
            String previousBookingStatus = null;

            if(selectedType == WorkOrderTypeEnum.DROP_IN){
                Vehicle vehicle = vehicleComboBox.getValue();
                List<ServiceItem> selectedService = serviceListView.getSelectionModel().getSelectedItems();

                if(vehicle == null || selectedService.isEmpty()){
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.noSelection").get());
                    return;
                }
                LocalDate date = datePicker.getValue();
                if (date == null) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.selectDate").get());
                    return;
                }
                LocalTime startTime;
                try {
                    startTime = LocalTime.parse(startTimeField.getText().trim());
                } catch (DateTimeParseException exception) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.invalidStartTime").get());
                    return;
                }
                int durationMinutes;
                try {
                    durationMinutes = Integer.parseInt(durationField.getText().trim());
                } catch (NumberFormatException exception) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.invalidDuration").get());
                    return;
                }
                if (durationMinutes <= 0) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.durationGreaterThanZero").get());
                    return;
                }
                if (selectedServices.isEmpty()) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.selectService").get());
                    return;
                }
// Validering för krockande/överlappande bokningar
                boolean overlapping = bookingRepository.hasOverlappingBooking(mechanic.getId(), date, startTime, durationMinutes);
                if (overlapping) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.overlapping").get());
                    return;
                }
// Sätt värdena på ditt DTO så att din strategiklass kan läsa in dem till skuggbokningen
                dto.setVehicleId(vehicle.getId());
                dto.setDate(date);
                dto.setStartTime(startTime);
                dto.setDuration(durationMinutes);

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
                if(booking != null) {
                    workOrderRepository.save(workOrder);
                    bookingRepository.save(booking);
                } else {
                    Booking autoBooking = Database.getBookings().stream()
                            .filter(b -> b.getVehicleId() == dto.getVehicleId() && "BOOKED".equals(b.getStatus()))
                            .findFirst().orElse(null);
                    if(autoBooking != null){
                        bookingRepository.save(autoBooking);
                        workOrder.setBookingId(autoBooking.getId());
                        workOrderRepository.save(workOrder);
                    }
                }
                if (!Database.getWorkOrders().contains(workOrder)) {
                    Database.getWorkOrders().add(workOrder);
                }
            } catch (RuntimeException exception) {
                // Tar bort arbetsordern ur minnet och återställer bokningen
                // om databassparandet misslyckas.
                Database.getWorkOrders().remove(workOrder);
                if(booking != null){
                    booking.setStatus(previousBookingStatus);
                } else {
                    Database.getBookings().removeIf(b -> b.getVehicleId() == dto.getVehicleId() && "BOOKED".equals(b.getStatus()));
                }

                UiKit.showError(feedbackLabel, language.text("createWorkOrder.saveError").get());
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(feedbackLabel, language.text("createWorkOrder.success").get());

            //tömmer valen efter registreringen har lyckats.
            bookingComboBox.getSelectionModel().clearSelection();
            bookingComboBox.setValue(null);
            mechanicComboBox.getSelectionModel().clearSelection();
            mechanicComboBox.setValue(null);
            typeComboBox.setValue(WorkOrderTypeEnum.PLANNED);
            vehicleCombo.getSelectionModel().clearSelection();
            vehicleCombo.setValue(null);
            datePicker.setValue(null);
            startTimeField.clear();
            serviceListView.getSelectionModel().clearSelection();

        });

        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader(language.text("createWorkOrder.title"), null),
                typeField,
                bookingField,
                vehicleField,
                dateTimeRow,
                durationMechanicRow,
                priceField,
                serviceField,
                mechanicField,
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

    private StringConverter<Vehicle> vehicleConverter() {
        return new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                String customer = Database.getCustomers().stream()
                        .filter(c -> c.getId() == vehicle.getCustomerId())
                        .map(Customer::getName).findFirst().orElse("");
                return vehicle == null ? "" : vehicle.getRegistrationNumber() + " - "
                        + vehicle.getModel() + " " + " (" +customer + ")";
            }
            @Override
            public Vehicle fromString(String text) {
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
