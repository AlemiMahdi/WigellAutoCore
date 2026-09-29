package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.entity.BookingEntity;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.ui.Navigator;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

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
import java.time.LocalTime;

import java.util.HashMap;
import java.util.Map;

// Visar alla bokningar i en tabell: fordon, datum, mekaniker, beskrivning och status.
public class ShowBookingsView {

    private final LanguageManager language = LanguageManager.getInstance();

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
                    language.text("bookings.loadError").get()
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

        TableColumn<Booking, String> startTimeColumn = new TableColumn<>("Start time");
        startTimeColumn.setCellValueFactory(cell -> {
            LocalTime startTime = cell.getValue().getStartTime();
            return new ReadOnlyStringWrapper(startTime == null ? "—" : startTime.toString());
        });
        startTimeColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> durationColumn = new TableColumn<>("Duration");
        durationColumn.setCellValueFactory(cell -> {
            Integer duration = cell.getValue().getDurationMinutes();
            return new ReadOnlyStringWrapper(duration == null || duration <= 0 ? "—" : duration + " min");
        });
        durationColumn.getStyleClass().add("cell-muted");


        TableColumn<Booking, String> mechanicColumn = new TableColumn<>("Mechanic");
        mechanicColumn.setCellValueFactory(cell -> {
            int bookingId = cell.getValue().getId();

            if (!mechanicByBooking.containsKey(bookingId)) {
                return new ReadOnlyStringWrapper(language.text("bookings.notSaved").get());
            }

            Integer mechanicId = mechanicByBooking.get(bookingId);

            if (mechanicId == null) {
                return new ReadOnlyStringWrapper(language.text("bookings.notAssigned").get());
            }

            String mechanicName = mechanicNames.get(mechanicId);

            if (mechanicName == null) {
                return new ReadOnlyStringWrapper(
                        language.text("bookings.unknownMechanic").get() + " (ID: " + mechanicId + ")"
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

        vehicleColumn.textProperty().bind(language.text("bookings.vehicle"));
        dateColumn.textProperty().bind(language.text("bookings.date"));
        startTimeColumn.textProperty().bind(language.text("bookings.startTime"));
        durationColumn.textProperty().bind(language.text("bookings.duration"));
        mechanicColumn.textProperty().bind(language.text("bookings.mechanic"));
        descriptionColumn.textProperty().bind(language.text("bookings.description"));
        statusColumn.textProperty().bind(language.text("bookings.status"));

        table.getColumns().add(vehicleColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(startTimeColumn);
        table.getColumns().add(durationColumn);
        table.getColumns().add(mechanicColumn);
        table.getColumns().add(descriptionColumn);
        table.getColumns().add(statusColumn);

        // Beskrivningen får mest plats, status minst
        vehicleColumn.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        dateColumn.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        startTimeColumn.setMaxWidth(1f * Integer.MAX_VALUE * 9);
        durationColumn.setMaxWidth(1f * Integer.MAX_VALUE * 9);
        mechanicColumn.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        descriptionColumn.setMaxWidth(1f * Integer.MAX_VALUE * 22);
        statusColumn.setMaxWidth(1f * Integer.MAX_VALUE * 13);

        ObservableList<Booking> bookings =
                FXCollections.observableArrayList(Database.getBookings());

        table.setItems(bookings);
        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("bookings.empty"));
        table.setPlaceholder(emptyLabel);
        UiKit.styleTable(table);

        return new VBox(20, buildHeader(), table);
    }

    // Rubrik med "+ New booking" till höger som hoppar till formuläret
    private HBox buildHeader() {
        Button newBookingButton = UiKit.primaryButton("+ New booking");
        newBookingButton.textProperty().bind(language.text("bookings.new"));
        newBookingButton.setOnAction(event -> Navigator.goTo("create-booking"));

        return UiKit.pageHeader(language.text("bookings.title"), newBookingButton);
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
        return language.text("bookings.vehicleId").get() + " " + vehicleId;
    }
}
