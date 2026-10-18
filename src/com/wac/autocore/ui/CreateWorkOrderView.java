package com.wac.autocore.ui;

import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Customer;
import com.wac.autocore.DTO.WorkOrderDto;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.service.BookingService;
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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//Formulär för att skapa arbetsordrar.
public class CreateWorkOrderView  extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();
    private final ObservableList<ServiceItem> selectedServices = FXCollections.observableArrayList();
    private final BookingService bookingService;

    public CreateWorkOrderView( BookingService bookingService) {
        this.bookingService = bookingService;

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
                textProperty().unbind();
                if (empty || type == null) {
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

        DatePicker datePicker = new DatePicker();
        datePicker.promptTextProperty().bind(language.text("createBooking.datePrompt"));

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm");

        TextField durationField = new TextField();
        durationField.promptTextProperty().bind(language.text("createBooking.durationPrompt"));
        durationField.setEditable(false);
        durationField.setText("0");

        Label totalPriceLabel = new Label();

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
                textProperty().unbind();
                if (empty || type == null) {
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

        //ny för drop-in arbetsorder
        ComboBox<Vehicle> vehicleComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getVehicles()));
        vehicleComboBox.promptTextProperty().bind(language.text("createWorkOrder.selectVehicle"));
        vehicleComboBox.setConverter(vehicleConverter());
        UiKit.keepPromptWhenCleared(vehicleComboBox);

        Button CreateCustomerButton = UiKit.primaryButton("+ Customer");
        CreateCustomerButton.setTooltip(new Tooltip("Snabbregistrera ny kund"));
        CreateCustomerButton.setOnAction(event -> Navigator.goTo("create-customer"));

        Button CreateVehicleButton = UiKit.primaryButton("+ Vehicle");
        CreateVehicleButton.setTooltip(new Tooltip("Snabbregistrera nytt fordon på kund"));
        CreateVehicleButton.setOnAction(event -> Navigator.goTo("create-vehicle"));
        HBox vehicleRowContainer = UiKit.formRow(vehicleComboBox, CreateCustomerButton, CreateVehicleButton);

        VBox typeField = UiKit.formField(language.text("createWorkOrder.type"), typeComboBox);
        VBox vehicleField = UiKit.formField(language.text("createWorkOrder.vehicle"), vehicleRowContainer);
        VBox bookingField = UiKit.formField(language.text("createWorkOrder.bookingLabel"), bookingComboBox);
        VBox mechanicField = UiKit.formField(language.text("bookings.mechanic"), mechanicComboBox);
        VBox serviceField = UiKit.formField(language.text("createWorkOrder.service"), serviceList);
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
            } else {
                durationMechanicRow.getChildren().addAll(mechanicField);
            }
        });

        Button saveButton = UiKit.primaryButton("Create work order");
        saveButton.textProperty().bind(language.text("createWorkOrder.title"));

        saveButton.setOnAction(event -> {
            AutoCoreApp app = AutoCoreApp.getInstance();
            GarageSystem garageSystem = app.getGarageSystem();
            WorkOrderRepository workOrderRepository = app.getWorkOrderRepository();
            BookingRepository bookingRepository = app.getBookingRepository();
            Mechanic mechanic = mechanicComboBox.getValue();
            WorkOrderTypeEnum selectedType = typeComboBox.getValue();

            if (mechanic == null || selectedType == null) {
                UiKit.showError(feedbackLabel, language.text("createWorkOrder.missingSelection").get());

                return;
            }

          WorkOrder originalWorkOrder = originalWorkOrderCombo.getValue();

          if (selectedType == WorkOrderTypeEnum.COMPLAINT && originalWorkOrder == null) {
              UiKit.showError(
                  feedbackLabel,
                  language.text("createWorkOrder.missingOriginalWorkOrder").get()
              );
              return;
          } 
           // WorkOrderDto dto = new WorkOrderDto();
            WorkOrder workOrder = null;

            Booking booking = null;
            String previousBookingStatus = null;

            if(selectedType == WorkOrderTypeEnum.DROP_IN){
                Vehicle vehicle = vehicleComboBox.getValue();

                if(vehicle == null || selectedServices.isEmpty()){
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.noSelection").get());
                    return;
                }
                int customerId = (vehicle != null) ? vehicle.getCustomerId() : 0;

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
                boolean overlapping = bookingService.hasOverlappingBooking(mechanic.getId(), date, startTime, durationMinutes);
                if (overlapping) {
                    UiKit.showError(feedbackLabel, language.text("createBooking.overlapping").get());
                    return;
                }
// Sätt värdena på DTO så att strategiklass kan läsa in dem till autobokningen

                double totalPrice = selectedServices.stream()
                        .mapToDouble(ServiceItem::getPrice)
                        .sum();
                List<Integer> serviceIds = new ArrayList<>();
                for(ServiceItem item : selectedServices){
                    serviceIds.add(item.getId());
                }
                WorkOrderDto dto = new WorkOrderDto(
                        0, customerId, vehicle.getId(), "Drop-in",
                        serviceIds, mechanic.getId(), totalPrice
                );
                dto.setDate(date);
                dto.setStartTime(startTime);
                dto.setDuration(durationMinutes);
                //GarageSystem anropar WorkOrderTypeManager
                selectedType = typeComboBox.getValue();
                workOrder = garageSystem.createWorkOrder(dto, selectedType);

            } else {
                // Gäller PLANNED och COMPLAINT
                booking = bookingComboBox.getValue();
                if (booking == null) {
                    UiKit.showError(feedbackLabel, language.text("createWorkOrder.noSelection").get());
                    return;
                }

                previousBookingStatus = booking.getStatus();

                WorkOrderDto dto = new WorkOrderDto(booking.getId(), mechanic.getId());
                if (selectedType == WorkOrderTypeEnum.COMPLAINT) {
                    dto.setOriginalWorkOrderId(originalWorkOrder.getId());
                }
                workOrder = garageSystem.createWorkOrder(dto, selectedType);
            }

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
                // Reklamationen får en egen bokning.
                if (selectedType == WorkOrderTypeEnum.COMPLAINT) {
                    Booking complaintBooking = new Booking();

                    complaintBooking.setVehicleId(booking.getVehicleId());
                    complaintBooking.setDescription(booking.getDescription());

                    complaintBooking.setStatus("WORK_ORDER_CREATED");
                    complaintBooking.setMechanicId(mechanic.getId());

                    List<ServiceItem> complaintServices = new ArrayList<>();
                    int totalMinutes = 0;

                    for (Integer serviceId : workOrder.getServiceItemIds()) {
                        ServiceItem service = Database.getServiceItems().stream()
                                .filter(item -> Integer.valueOf(item.getId()).equals(serviceId))
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "editWorkOrderDraft.serviceMissing"
                                        )
                                );

                        complaintServices.add(service);
                        totalMinutes += service.getEstimatedMinutes();

                        complaintBooking.getFrozenPrice().add(
                                new InvoiceLine(service.getId(), service.getName(), 0.0)
                        );
                    }

                    complaintBooking.setServices(complaintServices);
                    complaintBooking.setDurationMinutes(totalMinutes);

                    workOrderRepository.saveWithBooking(workOrder, complaintBooking);

                    // Lägg till bokningen i minnet först efter lyckad sparning.
                    Database.getBookings().add(complaintBooking);

                } else if (booking != null) {
                    workOrderRepository.save(workOrder);
                    bookingRepository.save(booking);
                } else {
                    if (!Database.getBookings().isEmpty()) {
                        Booking autoBooking = Database.getBookings()
                                .get(Database.getBookings().size() - 1);

                        if (autoBooking != null
                                && autoBooking.getId() == workOrder.getBookingId()) {

                            workOrderRepository.saveDropInOrder(workOrder, autoBooking);

                            if (!Database.getBookings().contains(autoBooking)) {
                                Database.getBookings().add(autoBooking);
                            }
                        }
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
                    int autoBookingId = workOrder.getBookingId();
                    Database.getBookings().removeIf(b -> b.getId() == autoBookingId);
                }

                UiKit.showError(feedbackLabel, language.text("createWorkOrder.saveError").get());
                exception.printStackTrace();
                return;
            }

            //tömmer valen efter registreringen har lyckats.
            bookingComboBox.getSelectionModel().clearSelection();
            bookingComboBox.setValue(null);
            mechanicComboBox.getSelectionModel().clearSelection();
            mechanicComboBox.setValue(null);
            typeComboBox.setValue(WorkOrderTypeEnum.PLANNED);
            originalWorkOrderCombo.getSelectionModel().clearSelection();
            originalWorkOrderCombo.setValue(null);
            vehicleComboBox.getSelectionModel().clearSelection();
            vehicleComboBox.setValue(null);
            datePicker.setValue(null);
            startTimeField.clear();
            selectedServices.clear();

            UiKit.showSuccess(feedbackLabel, language.text("createWorkOrder.success").get());

        });

        getChildren().add(UiKit.formContainer(
            UiKit.pageHeader(language.text("createWorkOrder.title"), null),
            typeField,
            originalWorkOrderField,
            bookingField,
            vehicleField,
            dateTimeRow,
            durationMechanicRow,
            priceField,
            serviceField,
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
                if (vehicle == null) {
                    return "";
                }
                String customer = Database.getCustomers().stream()
                        .filter(c -> c.getId() == vehicle.getCustomerId())
                        .map(Customer::getName).findFirst().orElse("");
                return vehicle.getRegistrationNumber() + " - "
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
