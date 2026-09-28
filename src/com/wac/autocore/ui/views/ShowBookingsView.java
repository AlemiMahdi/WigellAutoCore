package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
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
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.HashMap;
import java.util.Map;
import com.wac.autocore.ui.language.LanguageManager;

public class ShowBookingsView {

    public VBox getView() {

        LanguageManager language = LanguageManager.getInstance();

        Map<Integer, Integer> mechanicByBooking = new HashMap<>();
        Map<Integer, String> mechanicNames = new HashMap<>();

        for (Mechanic mechanic : Database.getMechanics()) {
            mechanicNames.put(mechanic.getId(), mechanic.getName());
        }
        try {BookingRepository bookingRepository = new BookingRepository();

            for(BookingEntity booking : bookingRepository.findAll()){
                mechanicByBooking.put(booking.getId(), booking.getMechanicId());
            }
        } catch (RuntimeException e) {
           e.printStackTrace();

            Label errorLabel = new Label();
            errorLabel.textProperty().bind(
                    language.text("bookings.loadError")
            );
           errorLabel.setWrapText(true);

           VBox errorView = new VBox(15, errorLabel);
           errorView.setPadding(new Insets(15));
           return errorView;
        }


        Label title = new Label("BOOKINGS");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        TableView<Booking> table = new TableView<>();

        TableColumn<Booking, Integer> idColumn =
                new TableColumn<>("ID");

        idColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, Integer>("id")
        );

        TableColumn<Booking, Integer> vehicleColumn =
                new TableColumn<>("Vehicle ID");

        vehicleColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, Integer>("vehicleId")
        );

        TableColumn<Booking, LocalDate> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, LocalDate>("date")
        );

        TableColumn<Booking, LocalTime> startTimeColumn =
        new TableColumn<>("Start time");

        startTimeColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, LocalTime>("startTime")
        );

        TableColumn<Booking, Integer> durationColumn =
                new TableColumn<>("Duration");

        durationColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, Integer>("durationMinutes")
        );

        TableColumn<Booking, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>("description")
        );

        TableColumn<Booking, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(cell ->
                language.text("booking.status." + cell.getValue().getStatus())
        );

        TableColumn<Booking, String> mechanicColumn =
                new TableColumn<>("Mechanic");

        mechanicColumn.setCellValueFactory(cell -> {
            int bookingId = cell.getValue().getId();

            if (!mechanicByBooking.containsKey(bookingId)) {
                return language.text("bookings.notSaved");
            }

            Integer mechanicId = mechanicByBooking.get(bookingId);

            if (mechanicId == null) {
                return language.text("bookings.notAssigned");
            }

            String mechanicName = mechanicNames.get(mechanicId);

            if (mechanicName == null) {
                return language.text("bookings.unknownMechanic")
                        .concat(" (ID: ")
                        .concat(String.valueOf(mechanicId))
                        .concat(")");
            }

            return new ReadOnlyStringWrapper(
                    mechanicName + " (ID: " + mechanicId + ")"
            );
        });

        // Uppdaterar rubrikerna direkt vid språkbyte.
        title.textProperty().bind(language.text("bookings.title"));
        idColumn.textProperty().bind(language.text("bookings.id"));
        vehicleColumn.textProperty().bind(language.text("bookings.vehicleId"));
        dateColumn.textProperty().bind(language.text("bookings.date"));
        startTimeColumn.textProperty().bind(language.text("bookings.startTime"));
        durationColumn.textProperty().bind(language.text("bookings.duration"));
        mechanicColumn.textProperty().bind(language.text("bookings.mechanic"));
        descriptionColumn.textProperty().bind(language.text("bookings.description"));
        statusColumn.textProperty().bind(language.text("bookings.status"));

        table.getColumns().addAll(
                idColumn,
                vehicleColumn,
                dateColumn,
                startTimeColumn,
                durationColumn,
                mechanicColumn,
                descriptionColumn,
                statusColumn
        );



        ObservableList<Booking> bookings =
                FXCollections.observableArrayList(
                        Database.getBookings()
                );

        table.setItems(bookings);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("bookings.empty"));
        table.setPlaceholder(emptyLabel);

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );


        VBox view = new VBox(15);

        view.setPadding(new Insets(10));

        view.getChildren().addAll(
                title,
                table
        );

        return view;
    }
}