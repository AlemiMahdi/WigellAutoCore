package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

//Formulär för att skapa arbetsordrar.
public class CreateWorkOrderView  extends VBox {

    // En CheckBox per tjänst. Tjänsten sparas i CheckBoxens userData
    // så att vi vet vilket ID som hör till en ikryssad ruta.
    private final List<CheckBox> serviceCheckBoxes = new ArrayList<>();

    public CreateWorkOrderView() {
        // Centrerar formulärkolumnen i innehållsytan
        setAlignment(Pos.TOP_CENTER);

        ComboBox<Booking> bookingComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getBookings()));
        bookingComboBox.setPromptText("Select booking");
        bookingComboBox.setConverter(bookingConverter());
        UiKit.keepPromptWhenCleared(bookingComboBox);

        ComboBox<Mechanic> mechanicComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getMechanics()));
        mechanicComboBox.setPromptText("Select mechanic");
        mechanicComboBox.setConverter(mechanicConverter());
        UiKit.keepPromptWhenCleared(mechanicComboBox);

        Button saveButton = UiKit.primaryButton("Create work order");

        Label feedbackLabel = UiKit.feedbackLabel();

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();

        saveButton.setOnAction(event -> {
            Booking booking = bookingComboBox.getValue();
            Mechanic mechanic = mechanicComboBox.getValue();

            if (booking == null || mechanic == null){
                UiKit.showError(feedbackLabel, "Please select a Booking and a Mechanic");

                return;
            }

            int[] serviceIds = selectedServiceIds();

            // Sparar bokningens status ifall vi behöver återställa den.
            String previousBookingStatus = booking.getStatus();

            //GarageSystem avgör om arbetsordern får skapas eller inte.
            WorkOrder workOrder = garageSystem.createWorkOrder(booking.getId(), mechanic.getId(), serviceIds);

            if(workOrder == null){
                if (!mechanic.isAvailable()){
                    UiKit.showError(feedbackLabel, "Mechanic is not available");
                }
                else {
                    UiKit.showError(feedbackLabel, "Work order could not be created. Please try again");
                }
                return;
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

                UiKit.showError(feedbackLabel, "Work order could not be saved. Please try again.");
                exception.printStackTrace();
                return;
            }

            UiKit.showSuccess(feedbackLabel, "Work order has been created");

            //Jag tömmer valen efter registreringen har lyckats.
            bookingComboBox.getSelectionModel().clearSelection();
            bookingComboBox.setValue(null);
            mechanicComboBox.getSelectionModel().clearSelection();
            mechanicComboBox.setValue(null);
            for (CheckBox checkBox : serviceCheckBoxes) {
                checkBox.setSelected(false);
            }

        });

        getChildren().add(UiKit.formContainer(
                UiKit.pageHeader("Create work order", null),
                UiKit.formField("Booking", bookingComboBox),
                UiKit.formField("Mechanic", mechanicComboBox),
                UiKit.formField("Services", buildServiceChecklist()),
                saveButton,
                feedbackLabel
        ));
    }

    // Inramad ruta med en CheckBox per tjänst, t.ex. "Tire rotation — 249 SEK".
    // Checkboxar är enklare än en flervals-lista där man måste hålla Ctrl.
    private VBox buildServiceChecklist() {
        for (ServiceItem serviceItem : Database.getServiceItems()) {
            CheckBox checkBox = new CheckBox(serviceItem.getName() + " — " + formatPrice(serviceItem.getPrice()));
            checkBox.setUserData(serviceItem);
            serviceCheckBoxes.add(checkBox);
        }

        VBox checklist = UiKit.checklistBox(serviceCheckBoxes.toArray(new CheckBox[0]));
        if (serviceCheckBoxes.isEmpty()) {
            checklist.getChildren().add(UiKit.emptyText("No items found."));
        }
        return checklist;
    }

    // Samlar ID:na för alla ikryssade tjänster, i samma form som GarageSystem vill ha (int[])
    private int[] selectedServiceIds() {
        List<Integer> ids = new ArrayList<>();
        for (CheckBox checkBox : serviceCheckBoxes) {
            if (checkBox.isSelected()) {
                ServiceItem serviceItem = (ServiceItem) checkBox.getUserData();
                ids.add(serviceItem.getId());
            }
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    // 249.0 visas som "249 SEK", 249.5 som "249.50 SEK"
    private String formatPrice(double price) {
        if (price == Math.rint(price)) {
            return String.format("%.0f SEK", price);
        }
        return String.format("%.2f SEK", price);
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

    private String registrationNumber(int vehicleId) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == vehicleId) {
                return vehicle.getRegistrationNumber();
            }
        }
        return "Vehicle ID " + vehicleId;
    }
}
