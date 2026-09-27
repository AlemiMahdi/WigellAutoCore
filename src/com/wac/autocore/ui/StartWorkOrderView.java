package com.wac.autocore.ui;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

//Visar arbetsordrar och startar vald order.
public class StartWorkOrderView extends VBox {


    public StartWorkOrderView() {
        setSpacing(10);

        Label titleLabel = new Label("Start Work Order");
        titleLabel.setStyle("-fx-font-size: 20");

        ComboBox<WorkOrder> workOrderComboBox = new ComboBox<>(FXCollections.observableArrayList(Database.getWorkOrders())

);
        workOrderComboBox.setPromptText("Select Work Order");
        workOrderComboBox.setMaxWidth(Double.MAX_VALUE);

        Button startButton = new Button("Start Work Order");

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();
        MechanicRepository mechanicRepository = new MechanicRepository();

        if (workOrderComboBox.getItems().isEmpty()) {
            feedbackLabel.setText("No Work Order has been found. Please create a work order first.");
            startButton.setDisable(true);
        }
        startButton.setOnAction(event -> {
            WorkOrder workorder = workOrderComboBox.getValue();
            if (workorder == null) {
                feedbackLabel.setText("Please select a Work Order.");
                return;
            }


            // Sparar den valda arbetsordern innan vi försöker starta den.
            String previousStatus = workorder.getStatus();

            garageSystem.startWorkOrder(workorder.getId());

            if ("CREATED".equals(previousStatus)
                    && "IN_PROGRESS".equals(workorder.getStatus())) {
                // Hämtar bokningen och mekanikern som startWorkOrder har ändrat.
                Booking booking = Database.getBookings().stream()
                        .filter(b -> b.getId() == workorder.getBookingId())
                        .findFirst()
                        .orElse(null);

                Mechanic mechanic = Database.getMechanics().stream()
                        .filter(m -> m.getId() == workorder.getMechanicId())
                        .findFirst()
                        .orElse(null);

                try {
                    // Sparar arbetsorderns, bokningens och mekanikerns nya status.
                    workOrderRepository.save(workorder);

                    if (booking != null) {
                        bookingRepository.save(booking);
                    }

                    if (mechanic != null) {
                        mechanicRepository.save(mechanic);
                    }

                    feedbackLabel.setText(
                            "Work order " + workorder.getId() + " has been started.");
                } catch (RuntimeException exception) {
                    feedbackLabel.setText(
                            "Work order was started but could not be saved.");
                    exception.printStackTrace();
                }

            } else  {
                feedbackLabel.setText("Work order cannot be started.");
            }

            //Återställer val och laddar om lista
            workOrderComboBox.getSelectionModel().clearSelection();
            workOrderComboBox.setValue(null);
            workOrderComboBox.setItems(FXCollections.observableArrayList(Database.getWorkOrders()));

        });

        getChildren().addAll(titleLabel, new Label("Work order:"), workOrderComboBox, feedbackLabel, startButton);



    }
}
