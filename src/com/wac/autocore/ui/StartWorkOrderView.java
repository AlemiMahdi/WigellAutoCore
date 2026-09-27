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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

// Start work order: lista med arbetsordrar till vänster och detaljer om vald order till höger.
public class StartWorkOrderView extends VBox {

    private final GarageSystem garageSystem = new GarageSystem();
    private final WorkOrderRepository workOrderRepository = new WorkOrderRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final MechanicRepository mechanicRepository = new MechanicRepository();

    // Vänster spalt (korten) och höger spalt (detaljkortet)
    private final VBox listColumn = new VBox(12);
    private final VBox detailColumn = new VBox(14);

    // Meddelandet ligger utanför korten så att det syns kvar när listan laddas om
    private final Label feedbackLabel = UiKit.feedbackLabel();

    // Den order användaren har klickat på (null = ingen vald)
    private WorkOrder selectedWorkOrder;
    private VBox selectedCard;

    public StartWorkOrderView() {
        setSpacing(20);

        // Båda spalterna är lika breda, precis som i designen
        HBox columns = new HBox(20, listColumn, detailColumn);
        HBox.setHgrow(listColumn, Priority.ALWAYS);
        HBox.setHgrow(detailColumn, Priority.ALWAYS);
        listColumn.setMaxWidth(Double.MAX_VALUE);
        detailColumn.setMaxWidth(Double.MAX_VALUE);
        listColumn.setPrefWidth(0);
        detailColumn.setPrefWidth(0);

        // Spalterna fyller resten av höjden så att detaljkortet når ner till botten (som i designen)
        VBox.setVgrow(columns, Priority.ALWAYS);

        getChildren().addAll(UiKit.pageHeader("Start work order", null), columns);

        // Första ordern väljs direkt när sidan öppnas, så att detaljerna syns
        reloadList(true);
    }

    // Bygger om vänsterlistan från Database och nollställer valet.
    private void reloadList(boolean selectFirst) {
        listColumn.getChildren().clear();
        selectedWorkOrder = null;
        selectedCard = null;

        // Bara ordrar med status CREATED kan startas, så bara de visas i listan
        List<WorkOrder> startable = new ArrayList<WorkOrder>();
        for (WorkOrder workOrder : Database.getWorkOrders()) {
            if ("CREATED".equals(workOrder.getStatus())) {
                startable.add(workOrder);
            }
        }

        if (Database.getWorkOrders().isEmpty()) {
            showEmptyState("No Work Order has been found. Please create a work order first.");
            return;
        }
        if (startable.isEmpty()) {
            showEmptyState("All work orders have already been started. Create a new work order to start one.");
            return;
        }

        for (WorkOrder workOrder : startable) {
            listColumn.getChildren().add(createWorkOrderCard(workOrder));
        }

        if (selectFirst) {
            selectWorkOrder(startable.get(0), (VBox) listColumn.getChildren().get(0));
        } else {
            showDetails();
        }
    }

    // Visas när det inte finns något att starta – med en genväg till "Create work order".
    private void showEmptyState(String message) {
        Button newButton = UiKit.primaryButton("+ New work order");
        newButton.setOnAction(event -> Navigator.goTo("create-work-order"));

        VBox emptyCard = UiKit.card(UiKit.emptyText(message), newButton);
        emptyCard.getStyleClass().add("detail-card");
        listColumn.getChildren().add(emptyCard);

        detailColumn.getChildren().setAll(feedbackLabel);
    }

    // Ett klickbart kort: "WO-5", "ABC123 · Volvo V70" och status-badge.
    private VBox createWorkOrderCard(WorkOrder workOrder) {
        Label title = new Label(formatId(workOrder));
        title.getStyleClass().add("select-card-title");

        Label vehicleLabel = new Label(formatVehicle(findVehicle(workOrder), " · "));
        vehicleLabel.getStyleClass().add("select-card-sub");

        HBox badgeRow = new HBox(UiKit.statusBadge(workOrder.getStatus()));
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = UiKit.selectCard(title, vehicleLabel, badgeRow);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(event -> selectWorkOrder(workOrder, card));
        return card;
    }

    // Flyttar markeringen till det klickade kortet och visar dess detaljer.
    private void selectWorkOrder(WorkOrder workOrder, VBox card) {
        if (selectedCard != null) {
            UiKit.setSelected(selectedCard, false);
        }
        selectedWorkOrder = workOrder;
        selectedCard = card;
        UiKit.setSelected(card, true);

        feedbackLabel.setText("");
        showDetails();
    }

    // Fyller högerspalten: detaljkort för vald order, eller en uppmaning att välja.
    private void showDetails() {
        Button startButton = UiKit.primaryButton("Start work order");
        startButton.setOnAction(event -> startSelectedWorkOrder());

        VBox detailCard;
        if (selectedWorkOrder == null) {
            detailCard = UiKit.card(
                    UiKit.emptyText("Select a work order in the list to see its details."),
                    startButton);
        } else {
            Booking booking = findBooking(selectedWorkOrder);
            Mechanic mechanic = findMechanic(selectedWorkOrder);

            Label heading = new Label(formatId(selectedWorkOrder) + " · "
                    + formatVehicle(findVehicle(selectedWorkOrder), " "));
            heading.getStyleClass().add("card-heading");
            heading.setWrapText(true);

            Label bookingLabel = detailText("Booking: " + formatBooking(booking));
            Label mechanicLabel = detailText("Mechanic: "
                    + (mechanic == null ? "Not assigned" : mechanic.getName()));
            Label servicesLabel = detailText("Services: " + formatServices(selectedWorkOrder));

            detailCard = UiKit.card(heading, bookingLabel, mechanicLabel, servicesLabel, startButton);
        }
        detailCard.getStyleClass().add("detail-card");
        // Kortet fyller höjden som i designen (vgrow i spalten)
        VBox.setVgrow(detailCard, Priority.ALWAYS);
        detailCard.setMaxWidth(Double.MAX_VALUE);

        detailColumn.getChildren().setAll(detailCard, feedbackLabel);
    }

    // Samma logik som tidigare: starta via GarageSystem och spara via repositories.
    private void startSelectedWorkOrder() {
        WorkOrder workorder = selectedWorkOrder;
        if (workorder == null) {
            UiKit.showError(feedbackLabel, "Please select a Work Order.");
            return;
        }

        // Sparar den valda arbetsorderns status innan vi försöker starta den.
        String previousStatus = workorder.getStatus();

        garageSystem.startWorkOrder(workorder.getId());

        if ("CREATED".equals(previousStatus)
                && "IN_PROGRESS".equals(workorder.getStatus())) {
            // Hämtar bokningen och mekanikern som startWorkOrder har ändrat.
            Booking booking = findBooking(workorder);
            Mechanic mechanic = findMechanic(workorder);

            try {
                // Sparar arbetsorderns, bokningens och mekanikerns nya status.
                workOrderRepository.save(workorder);

                if (booking != null) {
                    bookingRepository.save(booking);
                }

                if (mechanic != null) {
                    mechanicRepository.save(mechanic);
                }

                UiKit.showSuccess(feedbackLabel,
                        "Work order " + workorder.getId() + " has been started.");
            } catch (RuntimeException exception) {
                UiKit.showError(feedbackLabel,
                        "Work order was started but could not be saved.");
                exception.printStackTrace();
            }

        } else {
            UiKit.showError(feedbackLabel, "Work order cannot be started.");
        }

        // Återställer valet och laddar om listan (startad order försvinner ur den).
        // Inget kort väljs automatiskt, så man startar inte nästa order av misstag.
        reloadList(false);
    }

    // ------------------------------------------------------------
    // Hjälpmetoder för att slå upp och formatera data
    // ------------------------------------------------------------

    private Label detailText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("detail-text");
        label.setWrapText(true);
        return label;
    }

    private String formatId(WorkOrder workOrder) {
        return "WO-" + workOrder.getId();
    }

    private Booking findBooking(WorkOrder workOrder) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == workOrder.getBookingId()) {
                return booking;
            }
        }
        return null;
    }

    private Mechanic findMechanic(WorkOrder workOrder) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == workOrder.getMechanicId()) {
                return mechanic;
            }
        }
        return null;
    }

    // Fordonet hittas via bokningen (arbetsordern känner bara till bokningens id)
    private Vehicle findVehicle(WorkOrder workOrder) {
        Booking booking = findBooking(workOrder);
        if (booking == null) {
            return null;
        }
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == booking.getVehicleId()) {
                return vehicle;
            }
        }
        return null;
    }

    // "DEF456 · Toyota Corolla" i listan, "DEF456 Toyota Corolla" i rubriken
    private String formatVehicle(Vehicle vehicle, String separator) {
        if (vehicle == null) {
            return "Unknown vehicle";
        }
        return vehicle.getRegistrationNumber() + separator
                + vehicle.getBrand() + " " + vehicle.getModel();
    }

    private String formatBooking(Booking booking) {
        if (booking == null) {
            return "Unknown booking";
        }
        return booking.getDescription() + " · Requested " + booking.getDate();
    }

    // Slår upp tjänsternas namn och sätter ihop dem: "Oil change, Brake service"
    private String formatServices(WorkOrder workOrder) {
        StringBuilder names = new StringBuilder();
        if (workOrder.getServiceItemIds() == null) {
            return "No services added";
        }
        for (Integer serviceId : workOrder.getServiceItemIds()) {
            for (ServiceItem item : Database.getServiceItems()) {
                if (item.getId() == serviceId) {
                    if (names.length() > 0) {
                        names.append(", ");
                    }
                    names.append(item.getName());
                }
            }
        }
        return names.length() == 0 ? "No services added" : names.toString();
    }
}
