package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;

import com.wac.autocore.model.WorkOrder;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;

import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MechanicScheduleView {

    private final LanguageManager language = LanguageManager.getInstance();
    private final Map<Integer, Integer> mechanicByBooking = new HashMap<>();

    public VBox getView() {

        if (!loadMechanicMap()) {
            Label errorLabel = UiKit.feedbackLabel();
            UiKit.showError(errorLabel,
                    language.text("schedule.loadError").get()
            );
            errorLabel.setWrapText(true);

            VBox errorView = new VBox(15, errorLabel);
            errorView.setPadding(new Insets(15));
            return errorView;
        }

        //Titel
        Node title = UiKit.pageHeader(language.text("schedule.title"), null);


        //Steg 2: rullista
        ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
        mechanicCombo.getItems().addAll(Database.getMechanics());
        mechanicCombo.promptTextProperty().bind(language.text("schedule.select"));

        Label countLabel = new Label();

        //Tabell
        TableView<Booking> table = new TableView<>();

        TableColumn<Booking, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Booking, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Booking, LocalTime> startTimeCol =
                new TableColumn<>("Start time");

        startTimeCol.setCellValueFactory(
                new PropertyValueFactory<>("startTime")
        );

        TableColumn<Booking, Integer> durationCol =
                new TableColumn<>("Duration");

        durationCol.setCellValueFactory(
                new PropertyValueFactory<>("durationMinutes")
        );

        TableColumn<Booking, Integer> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));

        TableColumn<Booking, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        idCol.textProperty().bind(language.text("schedule.id"));
        dateCol.textProperty().bind(language.text("schedule.date"));
        startTimeCol.textProperty().bind(language.text("schedule.startTime"));
        durationCol.textProperty().bind(language.text("schedule.duration"));
        vehicleCol.textProperty().bind(language.text("schedule.vehicle"));
        descCol.textProperty().bind(language.text("schedule.description"));
        statusCol.textProperty().bind(language.text("schedule.status"));

        table.getColumns().addAll(
                idCol,
                dateCol,
                startTimeCol,
                durationCol,
                vehicleCol,
                descCol,
                statusCol
        );
        UiKit.styleTable(table);
        statusCol.setCellFactory(UiKit.badgeCells());
        Label selectLabel = UiKit.emptyText("");
        selectLabel.textProperty().bind(language.text("schedule.select"));
        table.setPlaceholder(selectLabel);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        mechanicCombo.setOnAction(actionEvent -> {
            Mechanic selected = mechanicCombo.getValue();
            if (selected == null) {
                return;
            }
            Integer selectedId = selected.getId();

            List<Booking> result = Database.getBookings().stream()
                    .filter(booking -> selectedId.equals(mechanicByBooking.get(booking.getId())))
                    .sorted(
                            Comparator.comparing(Booking::getDate)
                                    .thenComparing(
                                            booking -> booking.getStartTime() == null
                                                    ? LocalTime.MIN
                                                    : booking.getStartTime()
                                    )
                    )
                    .collect(Collectors.toList());

            table.setItems(FXCollections.observableArrayList(result));
            if (result.isEmpty()) {
                Label emptyLabel = UiKit.emptyText("");
                emptyLabel.textProperty().bind(language.text("schedule.empty"));
                table.setPlaceholder(emptyLabel);

            }
            countLabel.setText(language.text("schedule.count").get() + " " + result.size());
        });

        VBox root = new VBox(10, title, mechanicCombo, table, countLabel);

        return root;


    }

    private boolean loadMechanicMap() {
        try {

            for (BookingEntity entity : new BookingRepository().findAll()) {
                if (entity.getMechanicId() != null) {
                    mechanicByBooking.put(entity.getId(), entity.getMechanicId());
                }
            }

            for (WorkOrder workOrder : Database.getWorkOrders()) {
                mechanicByBooking.put(workOrder.getBookingId(), workOrder.getMechanicId());
            }
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }

    }
}
