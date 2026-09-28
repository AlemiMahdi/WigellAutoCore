package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Visar arbetsordrarna som en kanban-tavla: CREATED, STARTED och COMPLETED.
public class ShowWorkOrdersView {

    private final LanguageManager language = LanguageManager.getInstance();

    public VBox getView() {

        VBox createdColumn = UiKit.kanbanColumn(language.text("badge.CREATED"), "grey");
        VBox startedColumn = UiKit.kanbanColumn(language.text("badge.IN_PROGRESS"), "yellow");
        VBox completedColumn = UiKit.kanbanColumn(language.text("badge.COMPLETED"), "green");

        // Sorterar varje arbetsorder till rätt kolumn utifrån dess status
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            String status = workOrder.getStatus() == null ? "" : workOrder.getStatus().toUpperCase();

            if (status.equals("IN_PROGRESS")) {
                startedColumn.getChildren().add(buildCard(workOrder));
            } else if (status.equals("COMPLETED")) {
                completedColumn.getChildren().add(buildCard(workOrder));
            } else {
                // CREATED (och okända statusar) hamnar i första kolumnen så inget försvinner
                createdColumn.getChildren().add(buildCard(workOrder));
            }
        }

        addEmptyTextIfNoCards(createdColumn);
        addEmptyTextIfNoCards(startedColumn);
        addEmptyTextIfNoCards(completedColumn);

        HBox board = new HBox(24, createdColumn, startedColumn, completedColumn);
        board.setAlignment(Pos.TOP_LEFT);

        return new VBox(20, UiKit.pageHeader(language.text("workOrders.title"), null), board);
    }

    // Ett kort per arbetsorder: WO-id, fordon, mekaniker och tjänster
    private VBox buildCard(WorkOrder workOrder) {
        Label idLabel = new Label("WO-" + workOrder.getId());
        idLabel.getStyleClass().add("kanban-card-title");

        Label vehicleLabel = new Label(vehicleText(workOrder.getBookingId()));
        vehicleLabel.getStyleClass().add("kanban-card-text");

        Label mechanicLabel = new Label(mechanicName(workOrder.getMechanicId()));
        mechanicLabel.getStyleClass().add("kanban-card-sub");

        Label servicesLabel = new Label(servicesText(workOrder.getServiceItemIds()));
        servicesLabel.getStyleClass().add("kanban-card-hint");
        servicesLabel.setWrapText(true);

        VBox card = new VBox(idLabel, vehicleLabel, mechanicLabel, servicesLabel);
        card.getStyleClass().add("kanban-card");
        return card;
    }

    // En kolumn med bara rubriken ser trasig ut – visa en dämpad text i stället
    private void addEmptyTextIfNoCards(VBox column) {
        // Första barnet är rubriken, så 1 barn betyder "inga kort"
        if (column.getChildren().size() == 1) {
            Label emptyLabel = UiKit.emptyText("");
            emptyLabel.textProperty().bind(language.text("workOrders.empty"));
            column.getChildren().add(emptyLabel);
        }
    }

    // Arbetsordern pekar på en bokning, som i sin tur pekar på fordonet
    private String vehicleText(int bookingId) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == bookingId) {
                for (Vehicle vehicle : Database.getVehicles()) {
                    if (vehicle.getId() == booking.getVehicleId()) {
                        return vehicle.getRegistrationNumber() + " · "
                                + vehicle.getBrand() + " " + vehicle.getModel();
                    }
                }
                return language.text("bookings.vehicleId").get() + " " + booking.getVehicleId();
            }
        }
        return language.text("workOrders.bookingId").get() + " " + bookingId;
    }

    private String mechanicName(int mechanicId) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == mechanicId) {
                return mechanic.getName();
            }
        }
        // Ingen mekaniker med det ID:t (t.ex. 0) = inte tilldelad ännu
        return language.text("bookings.notAssigned").get();
    }

    // Gör om listan med tjänste-ID:n till "Oil change, Brake pads"
    private String servicesText(List<Integer> serviceItemIds) {
        List<String> names = new ArrayList<>();
        if (serviceItemIds == null) {
            return language.text("workOrders.noServices").get();
        }
        for (Integer serviceItemId : serviceItemIds) {
            for (ServiceItem serviceItem : Database.getServiceItems()) {
                if (serviceItem.getId() == serviceItemId) {
                    names.add(serviceItem.getName());
                }
            }
        }
        if (names.isEmpty()) {
            return language.text("workOrders.noServices").get();
        }
        return String.join(", ", names);
    }
}
