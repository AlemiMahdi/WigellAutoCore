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
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import com.wac.autocore.ui.language.LanguageManager;

public class CompleteWorkOrderView {
    public static VBox build(){

        LanguageManager language = LanguageManager.getInstance();

        TableView<WorkOrder> tableView = new TableView<>();

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<WorkOrder, Integer> idCol = new TableColumn<>("Work order Id");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<WorkOrder, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell ->
                language.text("workOrder.status." + cell.getValue().getStatus())
        );

        tableView.getColumns().addAll(idCol, statusCol);

        ObservableList<WorkOrder> data = FXCollections.observableArrayList(Database.getWorkOrders());
        tableView.setItems(data);

        GarageSystem garageSystem = new GarageSystem();
        WorkOrderRepository workOrderRepository = new WorkOrderRepository();
        BookingRepository bookingRepository = new BookingRepository();
        MechanicRepository mechanicRepository = new MechanicRepository();

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        Label workOrderIdLabel = new Label("Work order Id:");
        TextField workOrderField = new TextField();

        form.add(workOrderIdLabel, 0, 0);
        form.add(workOrderField,1, 0);

        Button completeButton = new Button("Complete Work order");
        Label statusLabel = new Label();

        statusLabel.setWrapText(true);

// Uppdaterar texterna direkt när användaren byter språk.
        idCol.textProperty().bind(language.text("completeWorkOrder.id"));
        statusCol.textProperty().bind(language.text("completeWorkOrder.status"));
        workOrderIdLabel.textProperty().bind(
                language.text("completeWorkOrder.idLabel")
        );
        completeButton.textProperty().bind(
                language.text("completeWorkOrder.button")
        );

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("completeWorkOrder.empty"));
        tableView.setPlaceholder(emptyLabel);

        completeButton.setOnAction(actionEvent -> {
            int workOderId;

            try {
                workOderId = Integer.parseInt(workOrderField.getText());
            } catch (NumberFormatException e) {
                statusLabel.textProperty().bind(
                        language.text("completeWorkOrder.invalidId")
                );
                return;
            }

            WorkOrder foundOrder = null;

            for(WorkOrder order : Database.getWorkOrders()){
                if(order.getId() == workOderId) {
                    foundOrder = order;
                    break;
                }
            }
            if(foundOrder == null) {
                statusLabel.textProperty().bind(
                        language.text("completeWorkOrder.notFound")
                );
            } else if(!foundOrder.getStatus().equals("IN_PROGRESS")) {
                statusLabel.textProperty().bind(
                        language.text("completeWorkOrder.invalidStatus")
                );
            } else {
            garageSystem.completeWorkOrder(workOderId);

            WorkOrder completedOrder = foundOrder;

            // Hämtar bokningen och mekanikern som completeWorkOrder har ändrat.
            Booking booking = Database.getBookings().stream()
                    .filter(b -> b.getId() == completedOrder.getBookingId())
                    .findFirst()
                    .orElse(null);

            Mechanic mechanic = Database.getMechanics().stream()
                    .filter(m -> m.getId() == completedOrder.getMechanicId())
                    .findFirst()
                    .orElse(null);

            try {
                // Sparar arbetsorderns, bokningens och mekanikerns nya status.
                workOrderRepository.save(completedOrder);

                if (booking != null) {
                    bookingRepository.save(booking);
                }

                if (mechanic != null) {
                    mechanicRepository.save(mechanic);
                }

                statusLabel.textProperty().bind(
                        language.text("completeWorkOrder.success")
                );
                tableView.refresh();
                workOrderField.clear();
            } catch (RuntimeException exception) {
                statusLabel.textProperty().bind(
                        language.text("completeWorkOrder.error")
                );
                tableView.refresh();
                exception.printStackTrace();
            }
        }


        });

        VBox root = new VBox(15, tableView ,form, completeButton, statusLabel);
        root.setPadding(new Insets(20));

        return root;
    }
}
