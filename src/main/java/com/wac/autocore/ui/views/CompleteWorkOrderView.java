package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.ui.ErrorHandler;
import com.wac.autocore.ui.Navigator;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


// Complete work order: ett centrerat kort per påbörjad (STARTED) arbetsorder.
public class CompleteWorkOrderView {

    // Kortens maxbredd – ungefär som i designen
    private static final double CARD_MAX_WIDTH = 680;

    private final LanguageManager language = LanguageManager.getInstance();
    private final WorkOrderService workOrderService;


    // Meddelandet ligger ovanför korten så att det syns kvar när korten byggs om
    private final Label statusLabel = UiKit.feedbackLabel();
    private final VBox cardList = new VBox(20);
    private final VBox root;

    public CompleteWorkOrderView(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;

        statusLabel.setMaxWidth(CARD_MAX_WIDTH);
        // Tomt meddelande ska inte ta plats ovanför korten
        statusLabel.managedProperty().bind(statusLabel.textProperty().isNotEmpty());
        statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());

        cardList.setAlignment(Pos.TOP_CENTER);

        root = new VBox(16, statusLabel, cardList);
        root.setAlignment(Pos.TOP_CENTER);
        root.setMaxWidth(CARD_MAX_WIDTH);
        // Centrerar kolumnen i innehållsytan (som formulären)
        StackPane.setAlignment(root, Pos.TOP_CENTER);

        reloadCards();
    }

    // AutoCoreApp anropar build() – den skapar vyn och lämnar ut dess rot-VBox
//    public static VBox build() {
//        return new CompleteWorkOrderView().root;
//    }

    public VBox getView() {
        return this.root;
    }

    // Bygger ett kort per order som kan slutföras (status IN_PROGRESS).
    private void reloadCards() {
        cardList.getChildren().clear();

        for (WorkOrder order : UiKit.newestFirst(
                Database.getWorkOrders(),
                WorkOrder::getId)) {
            if (order.getStatus().canChangeTo(WorkOrderStatus.COMPLETED)) {
                Button completeButton = UiKit.successButton("Complete work order");
                completeButton.textProperty().bind(language.text("completeWorkOrder.button"));
                int workOrderId = order.getId();
                completeButton.setOnAction(event -> {
                    if (completeWorkOrder(workOrderId)) {
                        // Den slutförda ordern försvinner ur listan
                        reloadCards();
                    }
                });
                cardList.getChildren().add(createWorkOrderCard(order, completeButton));
            }
        }

        if (cardList.getChildren().isEmpty()) {
            cardList.getChildren().add(createEmptyCard());
        }
    }

    //Måste ändras hämta från BookingRepo findbooking
    // Ett kort: rubrik, STARTED-badge, mekaniker, tjänster som checkboxar och grön knapp.
    private VBox createWorkOrderCard(WorkOrder order, Button completeButton) {
        Booking booking = findBooking(order.getBookingId());

        Label heading = new Label("WO-" + order.getId() + " · " + formatVehicle(booking));
        heading.getStyleClass().add("card-heading");
        heading.setWrapText(true);

        HBox badgeRow = new HBox(UiKit.statusBadge(order.getStatus().name()));
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Mechanic mechanic = findMechanic(order.getMechanicId());
        Label mechanicLabel = new Label(language.text("createWorkOrder.mechanic").get() + " "
                + (mechanic == null ? language.text("bookings.notAssigned").get() : mechanic.getName()));
        mechanicLabel.getStyleClass().add("detail-text");

        // Checklistan är ett stöd för mekanikern – den stoppar inte slutförandet
        VBox checklist = new VBox(10);
        if (order.getServiceItemIds() != null) {
            for (Integer serviceId : order.getServiceItemIds()) {
                ServiceItem item = findServiceItem(serviceId);
                if (item != null) {
                    checklist.getChildren().add(new CheckBox(item.getName()));
                }
            }
        }
        if (checklist.getChildren().isEmpty()) {
            Label noServicesLabel = UiKit.emptyText("");
            noServicesLabel.textProperty().bind(language.text("completeWorkOrder.noServices"));
            checklist.getChildren().add(noServicesLabel);
        }

        HBox buttonRow = new HBox(completeButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = UiKit.card(heading, badgeRow, mechanicLabel, checklist, buttonRow);
        card.getStyleClass().add("detail-card");
        card.setMaxWidth(CARD_MAX_WIDTH);
        return card;
    }

    // Visas när ingen order är påbörjad – med en genväg till "Start work order".
    private VBox createEmptyCard() {
        Label heading = new Label("No started work orders");
        heading.textProperty().bind(language.text("completeWorkOrder.emptyTitle"));
        heading.getStyleClass().add("card-heading");

        Button startButton = UiKit.primaryButton("Go to Start work order");
        startButton.textProperty().bind(language.text("completeWorkOrder.goToStart"));
        startButton.setOnAction(event -> Navigator.goTo("start-work-order"));

        Label hintLabel = UiKit.emptyText("");
        hintLabel.textProperty().bind(language.text("completeWorkOrder.emptyHint"));

        VBox card = UiKit.card(heading, hintLabel, startButton);
        card.getStyleClass().add("detail-card");
        card.setMaxWidth(CARD_MAX_WIDTH);
        return card;
    }

    // Slutför ordern via WorkOrderService. Returnerar true om det lyckades.
    private boolean completeWorkOrder(int workOrderId) {
        statusLabel.textProperty().unbind();

        try {
            workOrderService.completeWorkOrder(workOrderId);
            UiKit.showSuccess(statusLabel, "");
            statusLabel.textProperty().bind(language.text("completeWorkOrder.success"));
            return true;
        } catch (IllegalArgumentException exception) {
            // Regelfel: servicen skickar en språknyckel (t.ex. fel status)
            UiKit.showError(statusLabel, "");
            statusLabel.textProperty().bind(language.text(exception.getMessage()));
            return false;
        } catch (RuntimeException exception) {
            ErrorHandler.show(statusLabel, exception, "completeWorkOrder.error", "work order " + workOrderId);
            return false;
        }
    }

    // ------------------------------------------------------------
    // Hjälpmetoder för att slå upp och formatera data
    // ------------------------------------------------------------

    private Booking findBooking(int bookingId) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == bookingId) {
                return booking;
            }
        }
        return null;
    }

    private Mechanic findMechanic(int mechanicId) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == mechanicId) {
                return mechanic;
            }
        }
        return null;
    }

    private ServiceItem findServiceItem(int serviceId) {
        for (ServiceItem item : Database.getServiceItems()) {
            if (item.getId() == serviceId) {
                return item;
            }
        }
        return null;
    }

    // "ABC123 Volvo V70" – fordonet hittas via bokningen
    private String formatVehicle(Booking booking) {
        if (booking != null) {
            for (Vehicle vehicle : Database.getVehicles()) {
                if (vehicle.getId() == booking.getVehicleId()) {
                    return vehicle.getRegistrationNumber() + " "
                            + vehicle.getBrand() + " " + vehicle.getModel();
                }
            }
        }
        return language.text("common.unknownVehicle").get();
    }
}
