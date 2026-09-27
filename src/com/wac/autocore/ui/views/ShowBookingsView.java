package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.ui.Navigator;
import com.wac.autocore.ui.UiKit;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

// Visar alla bokningar i en tabell: fordon, datum, mekaniker, beskrivning och status.
public class ShowBookingsView {

    public VBox getView() {

        // Mekanikern sparas bara i databasen (BookingEntity), inte i Booking-modellen.
        // Därför slår vi upp den via repositoryt och kopplar ihop med bokningens ID.
        Map<Integer, Integer> mechanicByBooking = new HashMap<>();
        Map<Integer, String> mechanicNames = new HashMap<>();

        for (Mechanic mechanic : Database.getMechanics()) {
            mechanicNames.put(mechanic.getId(), mechanic.getName());
        }
        try {
            BookingRepository bookingRepository = new BookingRepository();

            for (BookingEntity booking : bookingRepository.findAll()) {
                mechanicByBooking.put(booking.getId(), booking.getMechanicId());
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return buildErrorView(
                    "Bookings could not be loaded. Please reopen this page to try again."
            );
        }

        TableView<Booking> table = new TableView<>();

        // Vehicle: "ABC123 · Volvo V70" i stället för bara ett ID
        TableColumn<Booking, String> vehicleColumn = new TableColumn<>("Vehicle");
        vehicleColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(vehicleText(cell.getValue().getVehicleId()))
        );

        TableColumn<Booking, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDate();
            return new ReadOnlyStringWrapper(date == null ? "—" : date.toString());
        });
        dateColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> mechanicColumn = new TableColumn<>("Mechanic");
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

            return new ReadOnlyStringWrapper(mechanicName);
        });
        mechanicColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>("description")
        );
        descriptionColumn.getStyleClass().add("cell-muted");

        // Status visas som en färgad badge (BOOKED lila, STARTED gul osv.)
        TableColumn<Booking, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>("status")
        );
        statusColumn.setCellFactory(UiKit.<Booking>badgeCells());

        table.getColumns().add(vehicleColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(mechanicColumn);
        table.getColumns().add(descriptionColumn);
        table.getColumns().add(statusColumn);

        // Beskrivningen får mest plats, status minst
        vehicleColumn.setMaxWidth(1f * Integer.MAX_VALUE * 26);
        dateColumn.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        mechanicColumn.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        descriptionColumn.setMaxWidth(1f * Integer.MAX_VALUE * 27);
        statusColumn.setMaxWidth(1f * Integer.MAX_VALUE * 14);

        ObservableList<Booking> bookings =
                FXCollections.observableArrayList(Database.getBookings());

        table.setItems(bookings);
        table.setPlaceholder(new Label("No bookings found."));
        UiKit.styleTable(table);

        return new VBox(20, buildHeader(), table);
    }

    // Rubrik med "+ New booking" till höger som hoppar till formuläret
    private HBox buildHeader() {
        Button newBookingButton = UiKit.primaryButton("+ New booking");
        newBookingButton.setOnAction(event -> Navigator.goTo("create-booking"));
        return UiKit.pageHeader("Bookings", newBookingButton);
    }

    // Visas om bokningarna inte gick att läsa från databasen
    private VBox buildErrorView(String message) {
        Label errorLabel = UiKit.feedbackLabel();
        UiKit.showError(errorLabel, message);
        return new VBox(20, buildHeader(), UiKit.card(errorLabel));
    }

    // Gör om ett fordons-ID till "regnr · märke modell" för tabellen
    private String vehicleText(int vehicleId) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == vehicleId) {
                return vehicle.getRegistrationNumber() + " · "
                        + vehicle.getBrand() + " " + vehicle.getModel();
            }
        }
        // Fordonet finns inte längre – visa åtminstone ID:t
        return "Vehicle ID " + vehicleId;
    }
}
