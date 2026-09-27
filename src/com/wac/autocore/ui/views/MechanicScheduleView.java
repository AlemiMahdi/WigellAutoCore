package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.wac.autocore.ui.language.LanguageManager;

public class MechanicScheduleView {

    private final Map<Integer, Integer> mechanicByBooking = new HashMap<>();

    public VBox getView() {

        LanguageManager language = LanguageManager.getInstance();

        if (!loadMechanicMap()) {
            Label errorLabel = new Label();
            errorLabel.textProperty().bind(language.text("schedule.loadError"));
            errorLabel.setWrapText(true);

            VBox errorView = new VBox(15, errorLabel);
            errorView.setPadding(new Insets(15));
            return errorView;
        }

        //Titel
        Label title = new Label("MECHANIC SCHEDULE");
        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        //Steg 2: rullista
        ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
        mechanicCombo.getItems().addAll(Database.getMechanics());
        mechanicCombo.setPromptText("Select mechanic");

        Label countLabel = new Label();

        //Tabell
        TableView<Booking> table = new TableView<>();

        TableColumn<Booking, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Booking, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Booking, Integer> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));

        TableColumn<Booking, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Uppdaterar rubrikerna direkt vid språkbyte.
        title.textProperty().bind(language.text("schedule.title"));
        mechanicCombo.promptTextProperty().bind(language.text("schedule.select"));

        idCol.textProperty().bind(language.text("schedule.id"));
        dateCol.textProperty().bind(language.text("schedule.date"));
        vehicleCol.textProperty().bind(language.text("schedule.vehicle"));
        descCol.textProperty().bind(language.text("schedule.description"));
        statusCol.textProperty().bind(language.text("schedule.status"));

        table.getColumns().addAll(idCol, dateCol, vehicleCol, descCol, statusCol);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("schedule.select"));

        table.setPlaceholder(emptyLabel);        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);


        mechanicCombo.setOnAction(actionEvent -> {
            Mechanic selected = mechanicCombo.getValue();
            if (selected == null) {
                return;
            }
            Integer selectedId = selected.getId();

            List<Booking> result = Database.getBookings().stream()
                    .filter(booking -> selectedId.equals(mechanicByBooking.get(booking.getId())))
                    .sorted(Comparator.comparing(Booking::getDate))
                    .collect(Collectors.toList());

            table.setItems(FXCollections.observableArrayList(result));
            emptyLabel.textProperty().bind(language.text("schedule.empty"));

            countLabel.textProperty().bind(
                    language.text("schedule.count")
                            .concat(" ")
                            .concat(String.valueOf(result.size()))
            );
        });

        VBox root = new VBox(10, title, mechanicCombo, table, countLabel);
        root.setPadding(new Insets(15));
        return root;


    }
    private boolean loadMechanicMap(){
        try {

            for (BookingEntity entity : new BookingRepository().findAll()) {
                if (entity.getMechanicId() != null) {
                    mechanicByBooking.put(entity.getId(), entity.getMechanicId());
                }
            }
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }

    }
}
