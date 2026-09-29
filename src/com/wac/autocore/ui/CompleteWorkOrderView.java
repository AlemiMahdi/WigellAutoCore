package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.GarageSystem;
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
    private final GarageSystem garageSystem = new GarageSystem();
    private final WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();

    // Meddelandet ligger ovanför korten så att det syns kvar när korten byggs om
    private final Label statusLabel = UiKit.feedbackLabel();
    private final VBox cardList = new VBox(20);
    private final VBox root;

    // AutoCoreApp anropar build() – den skapar vyn och lämnar ut dess rot-VBox
    public static VBox build() {
        return new CompleteWorkOrderView().root;
    }

    private CompleteWorkOrderView() {
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

    // Bygger ett kort per order som kan slutföras (status IN_PROGRESS).
    private void reloadCards() {
        cardList.getChildren().clear();

        for (WorkOrder order : Database.getWorkOrders()) {
            if ("IN_PROGRESS".equals(order.getStatus())) {
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

    // Ett kort: rubrik, STARTED-badge, mekaniker, tjänster som checkboxar och grön knapp.
    private VBox createWorkOrderCard(WorkOrder order, Button completeButton) {
        Booking booking = findBooking(order.getBookingId());

        Label heading = new Label("WO-" + order.getId() + " · " + formatVehicle(booking));
        heading.getStyleClass().add("card-heading");
        heading.setWrapText(true);

        HBox badgeRow = new HBox(UiKit.statusBadge(order.getStatus()));
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

    // Samma kontroller och anrop som tidigare: hitta ordern, kolla status,
    // slutför via GarageSystem och spara via repositories. Returnerar true om det lyckades.
    private boolean completeWorkOrder(int workOderId) {
        WorkOrder foundOrder = null;

        for (WorkOrder order : Database.getWorkOrders()) {
            if (order.getId() == workOderId) {
                foundOrder = order;
                break;
            }
        }
        if (foundOrder == null) {
            UiKit.showError(statusLabel, language.text("completeWorkOrder.notFound").get());
            return false;
        }
        if (!foundOrder.getStatus().equals("IN_PROGRESS")) {
            UiKit.showError(statusLabel, language.text("completeWorkOrder.invalidStatus").get());
            return false;
        }

        garageSystem.completeWorkOrder(workOderId);

        // Hämtar bokningen och mekanikern som completeWorkOrder har ändrat.
        Booking booking = findBooking(foundOrder.getBookingId());
        Mechanic mechanic = findMechanic(foundOrder.getMechanicId());

        try {
            // Sparar arbetsorderns, bokningens och mekanikerns nya status.
            workOrderRepository.save(foundOrder);

            if (booking != null) {
                bookingRepository.save(booking);
            }

            if (mechanic != null) {
                mechanicRepository.save(mechanic);
            }

            UiKit.showSuccess(statusLabel, language.text("completeWorkOrder.success").get());
        } catch (RuntimeException exception) {
            UiKit.showError(statusLabel,
                    language.text("completeWorkOrder.error").get());
            exception.printStackTrace();
        }
        // Ordern är slutförd i minnet även om sparandet misslyckades, så korten byggs om
        return true;
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
