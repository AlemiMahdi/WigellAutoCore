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

public class ShowBookingsView {

    public VBox getView() {

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


        table.getColumns().addAll(
                idColumn,
                vehicleColumn,
                dateColumn,
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