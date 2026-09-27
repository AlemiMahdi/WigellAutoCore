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
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.HashMap;
import java.util.Map;

public class ShowBookingsView {

    public VBox getView() {

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

           Label errorLabel = new Label(
                   "Bookings could not be loaded. Please reopen this page to try again."
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

        TableColumn<Booking, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>("description")
        );

        TableColumn<Booking, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>("status")
        );

        TableColumn<Booking, String> mechanicColumn =
                new TableColumn<>("Mechanic");

        mechanicColumn.setCellValueFactory(cell -> {
            int bookingId = cell.getValue().getId();

            if (!mechanicByBooking.containsKey(bookingId)) {
                return new ReadOnlyStringWrapper("Booking not saved");
            }

            Integer mechanicId = mechanicByBooking.get(bookingId);

            if (mechanicId == null) {
                return new ReadOnlyStringWrapper("Not assigned");
            }

            String mechanicName = mechanicNames.get(mechanicId);

            if (mechanicName == null) {
                return new ReadOnlyStringWrapper(
                        "Unknown mechanic (ID: " + mechanicId + ")"
                );
            }

            return new ReadOnlyStringWrapper(
                    mechanicName + " (ID: " + mechanicId + ")"
            );
        });

        table.getColumns().addAll(
                idColumn,
                vehicleColumn,
                dateColumn,
                mechanicColumn,
                descriptionColumn,
                statusColumn
        );


        ObservableList<Booking> bookings =
                FXCollections.observableArrayList(
                        Database.getBookings()
                );

        table.setItems(bookings);

        table.setPlaceholder(
                new Label("No bookings found.")
        );

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