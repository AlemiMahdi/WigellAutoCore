package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

// Visar alla bokningar i en tabell: fordon, datum, mekaniker, beskrivning och status.
public class ShowBookingsView {

    private final LanguageManager language = LanguageManager.getInstance();

    public VBox getView() {

        Map<Integer, Integer> mechanicByBooking = new HashMap<>();
        Map<Integer, String> mechanicNames = new HashMap<>();

        for (Mechanic mechanic : Database.getMechanics()) {
            mechanicNames.put(
                    mechanic.getId(),
                    mechanic.getName()
            );
        }

        try {
            BookingRepository bookingRepository =
                    new BookingRepository();

            for (Booking booking : bookingRepository.findAll()) {
                mechanicByBooking.put(
                        booking.getId(),
                        booking.getMechanicId()
                );
            }

        } catch (RuntimeException e) {
            e.printStackTrace();

            return buildErrorView(
                    language.text("bookings.loadError").get()
            );
        }

        TableView<Booking> table = new TableView<>();

        // Vehicle: "ABC123 · Volvo V70" i stället för bara ett ID
        TableColumn<Booking, String> vehicleColumn =
                new TableColumn<>("Vehicle");

        vehicleColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        vehicleText(
                                cell.getValue().getVehicleId()
                        )
                )
        );

        TableColumn<Booking, String> dateColumn =
                new TableColumn<>("Date");

        dateColumn.setCellValueFactory(cell -> {
            LocalDate date = cell.getValue().getDate();

            return new ReadOnlyStringWrapper(
                    date == null
                            ? "—"
                            : date.toString()
            );
        });

        dateColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> startTimeColumn =
                new TableColumn<>("Start time");

        startTimeColumn.setCellValueFactory(cell -> {
            LocalTime startTime =
                    cell.getValue().getStartTime();

            return new ReadOnlyStringWrapper(
                    startTime == null
                            ? "—"
                            : startTime.toString()
            );
        });

        startTimeColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> durationColumn =
                new TableColumn<>("Duration");

        durationColumn.setCellValueFactory(cell -> {
            int duration =
                    cell.getValue().getDurationMinutes();

            return new ReadOnlyStringWrapper(
                    duration <= 0
                            ? "—"
                            : duration + " min"
            );
        });

        durationColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> mechanicColumn =
                new TableColumn<>("Mechanic");

        mechanicColumn.setCellValueFactory(cell -> {

            int bookingId =
                    cell.getValue().getId();

            if (!mechanicByBooking.containsKey(bookingId)) {
                return new ReadOnlyStringWrapper(
                        language.text(
                                "bookings.notSaved"
                        ).get()
                );
            }

            Integer mechanicId =
                    mechanicByBooking.get(bookingId);

            if (mechanicId == null) {
                return new ReadOnlyStringWrapper(
                        language.text(
                                "bookings.notAssigned"
                        ).get()
                );
            }

            String mechanicName =
                    mechanicNames.get(mechanicId);

            if (mechanicName == null) {
                return new ReadOnlyStringWrapper(
                        language.text(
                                "bookings.unknownMechanic"
                        ).get()
                                + " (ID: "
                                + mechanicId
                                + ")"
                );
            }

            return new ReadOnlyStringWrapper(
                    mechanicName
            );
        });

        mechanicColumn.getStyleClass().add("cell-muted");

        TableColumn<Booking, String> descriptionColumn =
                new TableColumn<>("Description");

        descriptionColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>(
                        "description"
                )
        );

        descriptionColumn.getStyleClass().add("cell-muted");

        // Status visas som en färgad badge.
        TableColumn<Booking, String> statusColumn =
                new TableColumn<>("Status");

        statusColumn.setCellValueFactory(
                new PropertyValueFactory<Booking, String>(
                        "status"
                )
        );

        statusColumn.setCellFactory(
                UiKit.<Booking>badgeCells()
        );

        vehicleColumn.textProperty().bind(
                language.text("bookings.vehicle")
        );

        dateColumn.textProperty().bind(
                language.text("bookings.date")
        );

        startTimeColumn.textProperty().bind(
                language.text("bookings.startTime")
        );

        durationColumn.textProperty().bind(
                language.text("bookings.duration")
        );

        mechanicColumn.textProperty().bind(
                language.text("bookings.mechanic")
        );

        descriptionColumn.textProperty().bind(
                language.text("bookings.description")
        );

        statusColumn.textProperty().bind(
                language.text("bookings.status")
        );

        table.getColumns().add(vehicleColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(startTimeColumn);
        table.getColumns().add(durationColumn);
        table.getColumns().add(mechanicColumn);
        table.getColumns().add(descriptionColumn);
        table.getColumns().add(statusColumn);

        // Beskrivningen får mest plats, status minst.
        vehicleColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 20
        );

        dateColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 12
        );

        startTimeColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 9
        );

        durationColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 9
        );

        mechanicColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 15
        );

        descriptionColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 22
        );

        statusColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 13
        );

        ObservableList<Booking> bookings =
                FXCollections.observableArrayList(
                        Database.getBookings()
                );

        table.setItems(bookings);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(
                language.text("bookings.empty")
        );

        table.setPlaceholder(emptyLabel);

        UiKit.styleTable(table);

        VBox root =
                new VBox(
                        20,
                        buildHeader(),
                        table
                );

        // Redigeringsknapp
        TableColumn<Booking, Void> editColumn =
                new TableColumn<>();

        editColumn.setSortable(false);
        editColumn.setMinWidth(60);
        editColumn.setPrefWidth(60);
        editColumn.setMaxWidth(60);

        editColumn.setCellFactory(
                column ->
                        new TableCell<Booking, Void>() {

                            private final Button editButton =
                                    UiKit.primaryButton("");

                            {
                                SVGPath pencil =
                                        new SVGPath();

                                pencil.setContent(
                                        "M 1 10 L 1 13 L 4 13 L 11 6 L 8 3 Z "
                                                + "M 9 2 L 11 0 L 14 3 L 12 5 Z"
                                );

                                pencil.getStyleClass().add(
                                        "edit-pencil"
                                );

                                editButton.setGraphic(
                                        pencil
                                );

                                editButton.getStyleClass().add(
                                        "icon-button"
                                );

                                Tooltip tooltip =
                                        new Tooltip();

                                tooltip.textProperty().bind(
                                        language.text(
                                                "bookings.edit"
                                        )
                                );

                                editButton.setTooltip(
                                        tooltip
                                );

                                editButton
                                        .accessibleTextProperty()
                                        .bind(
                                                language.text(
                                                        "bookings.edit"
                                                )
                                        );

                                editButton.setOnAction(
                                        event -> {

                                            int index =
                                                    getIndex();

                                            if (index < 0
                                                    || index >=
                                                    getTableView()
                                                            .getItems()
                                                            .size()) {
                                                return;
                                            }

                                            Booking booking =
                                                    getTableView()
                                                            .getItems()
                                                            .get(index);

                                            if (booking != null) {
                                                openBookingEditor(
                                                        root,
                                                        booking
                                                );
                                            }
                                        }
                                );

                                setAlignment(
                                        javafx.geometry.Pos.CENTER
                                );
                            }

                            @Override
                            protected void updateItem(
                                    Void item,
                                    boolean empty
                            ) {
                                super.updateItem(
                                        item,
                                        empty
                                );

                                setText(null);

                                setGraphic(
                                        empty
                                                ? null
                                                : editButton
                                );
                            }
                        }
        );

        table.getColumns().add(editColumn);

        // Dubbelklick på en bokning öppnar formuläret.
        table.setRowFactory(tableView -> {

            TableRow<Booking> row =
                    new TableRow<>();

            row.setOnMouseClicked(event -> {

                if (event.getButton()
                        == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {

                    openBookingEditor(
                            root,
                            row.getItem()
                    );
                }
            });

            return row;
        });

        return root;
    }

    // Används av både pennknappen och dubbelklicket.
    private void openBookingEditor(
            VBox root,
            Booking booking
    ) {

        Button backButton =
                UiKit.primaryButton("");

        backButton.textProperty().bind(
                language.text("bookings.back")
        );

        backButton.setOnAction(
                event ->
                        Navigator.goTo(
                                "show-bookings"
                        )
        );

        VBox editView =
                new EditBookingServicesView()
                        .getView(booking);

        root.getChildren().setAll(
                backButton,
                editView
        );
    }

    // Rubrik med "+ New booking" till höger.
    private HBox buildHeader() {

        Button newBookingButton =
                UiKit.primaryButton(
                        "+ New booking"
                );

        newBookingButton.textProperty().bind(
                language.text("bookings.new")
        );

        newBookingButton.setOnAction(
                event ->
                        Navigator.goTo(
                                "create-booking"
                        )
        );

        return UiKit.pageHeader(
                language.text("bookings.title"),
                newBookingButton
        );
    }

    // Visas om bokningarna inte gick att läsa från databasen.
    private VBox buildErrorView(
            String message
    ) {

        Label errorLabel =
                UiKit.feedbackLabel();

        UiKit.showError(
                errorLabel,
                message
        );

        return new VBox(
                20,
                buildHeader(),
                UiKit.card(errorLabel)
        );
    }

    // Gör om ett fordons-ID till "regnr · märke modell".
    private String vehicleText(
            int vehicleId
    ) {

        for (Vehicle vehicle :
                Database.getVehicles()) {

            if (vehicle.getId() == vehicleId) {

                return vehicle.getRegistrationNumber()
                        + " · "
                        + vehicle.getBrand()
                        + " "
                        + vehicle.getModel();
            }
        }

        // Fordonet finns inte längre – visa åtminstone ID:t.
        return language.text(
                "bookings.vehicleId"
        ).get()
                + " "
                + vehicleId;
    }
}