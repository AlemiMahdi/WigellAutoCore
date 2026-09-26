package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

//Formulär för att skapa arbetsordrar.
public class CreateWorkOrderView  extends VBox {

    public CreateWorkOrderView() {
        setSpacing(10);
        Label titleLabel = new Label("Create Work Order");
        titleLabel.setStyle("-fx-font-size: 20");

        ComboBox<Booking> bookingComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getBookings()));
        bookingComboBox.setPromptText("Select a Booking to Create");
        bookingComboBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Mechanic> mechanicComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getMechanics()));

        mechanicComboBox.setPromptText("Select a Mechanic");
        mechanicComboBox.setMaxWidth(Double.MAX_VALUE);
        ListView<ServiceItem> serviceItemListView = new ListView<>(FXCollections.observableArrayList(Database.getServiceItems()));

        serviceItemListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serviceItemListView.setPlaceholder(new Label("No items found."));

        Label help = new Label("Select services. To select multiple hold Command (Mac) or Ctrl (Windows)");
        help.setWrapText(true);

        Button saveButton = new Button("Save");

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();

        saveButton.setOnAction(event -> {
            Booking booking = bookingComboBox.getValue();
            Mechanic mechanic = mechanicComboBox.getValue();

            if (booking == null || mechanic == null){
                feedbackLabel.setText("Please select a Booking and a Mechanic");

                return;
            }

            int[] serviceIds = serviceItemListView.getSelectionModel().getSelectedItems().stream().mapToInt(ServiceItem::getId).toArray();

            // Sparar bokningens status ifall vi behöver återställa den.
            String previousBookingStatus = booking.getStatus();

            //GarageSystem avgör om arbetsordern får skapas eller inte.
            WorkOrder workOrder = garageSystem.createWorkOrder(booking.getId(), mechanic.getId(), serviceIds);

            if(workOrder == null){
                if (!mechanic.isAvailable()){
                    feedbackLabel.setText("Mechanic is not available");
                }
                else {
                    feedbackLabel.setText("Work order could not be created. Please try again");
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

                feedbackLabel.setText("Work order could not be saved. Please try again.");
                exception.printStackTrace();
                return;
            }

            feedbackLabel.setText("Work order has been created");

            //Jag tömmer valen efter registreringen har lyckats.
            bookingComboBox.getSelectionModel().clearSelection();
            bookingComboBox.setValue(null);
            mechanicComboBox.getSelectionModel().clearSelection();
            mechanicComboBox.setValue(null);
            serviceItemListView.getSelectionModel().clearSelection();

        });

        VBox.setVgrow(serviceItemListView, Priority.ALWAYS);
        getChildren().addAll(
                titleLabel,
                new Label("Booking:"),
                bookingComboBox,
                new Label("Mechanic"),
                mechanicComboBox,
                new Label("Services"),
                help,
                serviceItemListView,
                feedbackLabel,
                saveButton

        );



    }
}

