package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.security.actions.SecuredWorkOrderActions;
import org.springframework.security.access.AccessDeniedException;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;

// Visar arbetsordrarna som en kanban-tavla: CONFIRMED, IN_PROGRESS och COMPLETED.
public class ShowWorkOrdersView {

    private final LanguageManager language = LanguageManager.getInstance();
    private final SecuredWorkOrderActions securedWorkOrderActions;
    private boolean canEdit;
    private final BookingService bookingService;

    public ShowWorkOrdersView(
        SecuredWorkOrderActions securedWorkOrderActions,
        BookingService bookingService,
        boolean canEdit
    ) { 
        this.securedWorkOrderActions = securedWorkOrderActions;
        this.bookingService = bookingService;
        this.canEdit = canEdit;
    }

    public VBox getView() {

        VBox root = new VBox(20);

        VBox draftColumn = UiKit.kanbanColumn(language.text("workOrders.drafts"), "grey");
        VBox createdColumn = UiKit.kanbanColumn(language.text("badge.CREATED"), "grey");
        VBox startedColumn = UiKit.kanbanColumn(language.text("badge.IN_PROGRESS"), "yellow");
        VBox completedColumn = UiKit.kanbanColumn(language.text("badge.COMPLETED"), "green");

        // Sorterar varje arbetsorder till rätt kolumn utifrån dess status
        // Visa senaste arbetsordern överst i varje statuskolumn
        for (WorkOrder workOrder : UiKit.newestFirst(
                securedWorkOrderActions.findAll(),
                WorkOrder::getId
        )) {
            WorkOrderStatus status = workOrder.getStatus();

            if (status == WorkOrderStatus.DRAFT) {
                draftColumn.getChildren().add(buildCard(workOrder, root));
            } else if (status == WorkOrderStatus.IN_PROGRESS) {
                startedColumn.getChildren().add(buildCard(workOrder, root));
            } else if (status == WorkOrderStatus.COMPLETED) {
                completedColumn.getChildren().add(buildCard(workOrder, root));
            } else {
                // CONFIRM (och okända statusar) hamnar i första kolumnen så inget försvinner
                createdColumn.getChildren().add(buildCard(workOrder, root));
            }
        }

        addEmptyTextIfNoCards(createdColumn);
        addEmptyTextIfNoCards(draftColumn);
        addEmptyTextIfNoCards(startedColumn);
        addEmptyTextIfNoCards(completedColumn);

        HBox board = new HBox(24, draftColumn ,createdColumn, startedColumn, completedColumn);
        board.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(
                UiKit.pageHeader(language.text("workOrders.title"), null), board
        );
        return root;
    }

    // Ett kort per arbetsorder: WO-id, fordon, mekaniker och tjänster
    private VBox buildCard(WorkOrder workOrder, VBox root) {
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

        // Visar vilken arbetsorder reklamationen tillhör
        if (workOrder.getOriginalWorkOrderId() != null) {
            Label originalLabel = new Label();

            originalLabel.textProperty().bind(
                language.text("workOrders.complaintOf")
                    .concat(String.valueOf(workOrder.getOriginalWorkOrderId()))
            );

            originalLabel.getStyleClass().add("kanban-card-hint");
            originalLabel.setStyle("-fx-font-weight: bold;");
            card.getChildren().add(originalLabel);
        }

        // Visar vilka reklamationer som tillhör denna arbetsorder
        List<String> complaintIds = new ArrayList<>();

        for (WorkOrder other : Database.getWorkOrders()) {
            if (other.getOriginalWorkOrderId() != null
                    && other.getOriginalWorkOrderId() == workOrder.getId()) {
                complaintIds.add("WO-" + other.getId());
            }
        }

        if (!complaintIds.isEmpty()) {
            Label complaintsLabel = new Label();

            complaintsLabel.textProperty().bind(
                language.text("workOrders.relatedComplaints")
                    .concat(": " + String.join(", ", complaintIds))
            );

            complaintsLabel.getStyleClass().add("kanban-card-hint");
            complaintsLabel.setStyle("-fx-font-weight: bold;");
            complaintsLabel.setWrapText(true);

            card.getChildren().add(complaintsLabel);
        }

        card.getStyleClass().add("kanban-card");

        if (canEdit && workOrder.getStatus() == WorkOrderStatus.DRAFT) {
            Button editButton = UiKit.primaryButton("");
            editButton.textProperty().bind(
                    language.text("editWorkOrderDraft.open")
            );

            editButton.setOnAction(event -> {
                Button backButton = UiKit.primaryButton("");
                backButton.textProperty().bind(
                        language.text("editWorkOrderDraft.back")
                );

                backButton.setOnAction(backEvent ->
                        root.getChildren().setAll(getView())
                );

                VBox editView =
                        new EditWorkOrderDraftView(securedWorkOrderActions, bookingService).getView(workOrder);

                root.getChildren().setAll(backButton, editView);
            });

            Button confirmButton = UiKit.primaryButton("");
            confirmButton.textProperty().bind(
                    language.text("confirmWorkOrder.button")
            );

            VBox feedbackBox = new VBox(4);
            feedbackBox.setVisible(false);
            feedbackBox.setManaged(false);

            confirmButton.setOnAction(event -> {
                feedbackBox.getChildren().clear();
                feedbackBox.setVisible(false);
                feedbackBox.setManaged(false);

                List<String> errors;

            try {
                errors = securedWorkOrderActions.confirmWorkOrder(
                        workOrder.getId()
                );

            } catch (AccessDeniedException exception) {
                Label errorLabel = UiKit.feedbackLabel();
                errorLabel.setWrapText(true);

                UiKit.showError(errorLabel, "");
                errorLabel.textProperty().bind(
                        language.text("security.accessDenied")
                );

                feedbackBox.getChildren().add(errorLabel);
                feedbackBox.setVisible(true);
                feedbackBox.setManaged(true);
                return;

            } catch (RuntimeException exception) {
                Label errorLabel = UiKit.feedbackLabel();
                errorLabel.setWrapText(true);

                UiKit.showError(errorLabel, "");
                errorLabel.textProperty().bind(
                        language.text("confirmWorkOrder.saveError")
                );

                feedbackBox.getChildren().add(errorLabel);
                feedbackBox.setVisible(true);
                feedbackBox.setManaged(true);

                exception.printStackTrace();
                return;
            }


                if (!errors.isEmpty()) {
                    for (String key : errors) {
                        Label errorLabel = UiKit.feedbackLabel();
                        errorLabel.setWrapText(true);
                        UiKit.showError(errorLabel, "");
                        errorLabel.textProperty().bind(language.text(key));

                        feedbackBox.getChildren().add(errorLabel);
                    }

                    feedbackBox.setVisible(true);
                    feedbackBox.setManaged(true);
                    return;
                }

                Label successLabel = UiKit.feedbackLabel();
                successLabel.setWrapText(true);
                UiKit.showSuccess(successLabel, "");
                successLabel.textProperty().bind(
                        language.text("confirmWorkOrder.success")
                                .concat(" WO-" + workOrder.getId())
                );
                VBox refreshedView = getView();
                refreshedView.getChildren().add(1, successLabel);
                root.getChildren().setAll(refreshedView);
            });

            FlowPane actions = new FlowPane(8, 8);
            actions.getChildren().addAll(editButton, confirmButton);

            card.getChildren().addAll(actions, feedbackBox);
        }

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
