package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.security.actions.SecuredBookingActions;
import com.wac.autocore.service.BookingService;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Node;

// Visar alla bokningar i en tabell: fordon, datum, mekaniker, beskrivning och status.
public class ShowBookingsView {

        private final LanguageManager language = LanguageManager.getInstance();
        private final SecuredBookingActions securedBookingActions;
        private final boolean canEdit;

        public ShowBookingsView(
                SecuredBookingActions securedBookingActions,
                boolean canEdit
        ){
                this.securedBookingActions = securedBookingActions;
                this.canEdit = canEdit;
        }


    public VBox getView() {

        Map<Integer, Integer> mechanicByBooking = new HashMap<>();
        Map<Integer, String> mechanicNames = new HashMap<>();

        for (Mechanic mechanic : Database.getMechanics()) {
            mechanicNames.put(
                    mechanic.getId(),
                    mechanic.getName()
            );
        }

        List<Booking> loadedBookings;
        try {

                loadedBookings = securedBookingActions.getAllBookings();
                for(Booking booking : loadedBookings) {
                        mechanicByBooking.put(
                                booking.getId(), 
                                booking.getMechanicId());
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

        statusColumn.textProperty().bind(
                language.text("bookings.status")
        );

        table.getColumns().add(vehicleColumn);
        table.getColumns().add(dateColumn);
        table.getColumns().add(startTimeColumn);
        table.getColumns().add(durationColumn);
        table.getColumns().add(mechanicColumn);
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

        statusColumn.setMaxWidth(
                1f * Integer.MAX_VALUE * 13
        );

        ObservableList<Booking> bookings =
                UiKit.newestFirst(
                        loadedBookings,
                        Booking::getId
                );

        table.setItems(bookings);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(
                language.text("bookings.empty")
        );

        table.setPlaceholder(emptyLabel);

        UiKit.styleTable(table);

        VBox root = new VBox(20);

        // Redigeringsknapp
        TableColumn<Booking, Void> editColumn =
                new TableColumn<>();

        editColumn.textProperty().bind(
                language.text("bookings.actions")
        );
        editColumn.setSortable(false);
        editColumn.setMinWidth(360);
        editColumn.setPrefWidth(380);
        editColumn.setMaxWidth(420);

        editColumn.setCellFactory(
                column ->
                        new TableCell<Booking, Void>() {

                            private final Button editButton = new Button();

                            private final Button descriptionButton = new Button();

                            private final HBox actions =
                                    new HBox(8, descriptionButton, editButton);

                            {
                                descriptionButton.textProperty().bind(
                                        language.text("bookings.showDescription")
                                );

                                editButton.textProperty().bind(
                                        language.text("bookings.editServices")
                                );

                                descriptionButton.getStyleClass().add("booking-description-button");
                                editButton.getStyleClass().add("booking-edit-button");

                                actions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                                descriptionButton.setOnAction(event -> {
                                    int index = getIndex();

                                    if (index < 0
                                            || index >= getTableView().getItems().size()) {
                                        return;
                                    }

                                    Booking booking = getTableView().getItems().get(index);

                                    if (booking != null) {
                                        showDescription(root, booking);
                                    }
                                });

// Knappklick ska inte också öppna radens dubbelklicksredigering.
                                actions.addEventHandler(
                                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                                        event -> event.consume()
                                );

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

                                editButton
                                        .accessibleTextProperty()
                                        .bind(
                                                language.text(
                                                        "bookings.editServices"
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

                                setGraphic(empty ? null : actions);
                            }
                        }
        );

        if (canEdit) {
                table.getColumns().add(editColumn);    
        }
        

        // Skapar en ny bokning med den markerade som underlag.
        Button copyButton = UiKit.primaryButton("");
        copyButton.textProperty().bind(language.text("bookings.copy"));

// Knappen kan bara användas när en bokning är markerad.
        copyButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull()
        );

        copyButton.setOnAction(event -> {
            Booking selectedBooking =
                    table.getSelectionModel().getSelectedItem();

            if (selectedBooking != null) {
                showBookingForm(
                        root,
                        new CreateBookingView(securedBookingActions ,selectedBooking).getView()
                );
            }
        });

        root.getChildren().addAll(buildHeader(copyButton), table);

        // Dubbelklick på en bokning öppnar formuläret.
        if (canEdit) {
        table.setRowFactory(tableView -> {

                TableRow<Booking> row = new TableRow<>();

                row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2
                        && !row.isEmpty()) {

                        openBookingEditor(root, row.getItem());
                }
                });

                return row;
        });
        }

        return root;
    }

    private void showDescription(VBox root, Booking booking) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(root.getScene().getWindow());

        dialog.titleProperty().bind(
                language.text("bookings.description")
        );

        dialog.setHeaderText(
                vehicleText(booking.getVehicleId())
                        + " · #" + booking.getId()
                        + (booking.getDate() == null
                        ? ""
                        : " · " + booking.getDate())
        );

        TextArea description = new TextArea();
        description.setEditable(false);
        description.setWrapText(true);
        description.setPrefRowCount(8);

        String text = booking.getDescription();

        if (text == null || text.trim().isEmpty()) {
            description.textProperty().bind(
                    language.text("bookings.noDescription")
            );
        } else {
            description.setText(text);
        }

        dialog.getDialogPane().setContent(description);
        dialog.getDialogPane().setPrefWidth(560);
        dialog.getDialogPane().getStylesheets().addAll(
                root.getScene().getStylesheets()
        );

        dialog.getDialogPane().getButtonTypes().add(
                ButtonType.CLOSE
        );

        Button closeButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.CLOSE);

        closeButton.textProperty().bind(
                language.text("bookings.closeDescription")
        );

        dialog.setResizable(true);
        dialog.showAndWait();
    }

    // Används av både pennknappen och dubbelklicket.
    private void openBookingEditor(VBox root, Booking booking) {
        showBookingForm(
                root,
                new EditBookingServicesView(securedBookingActions).getView(booking)
        );
    }

    // Gemensam tillbaka-knapp för redigering och kopiering.
    private void showBookingForm(VBox root, Node form) {
        Button backButton = UiKit.primaryButton("");
        backButton.textProperty().bind(language.text("bookings.back"));
        backButton.setOnAction(event ->
                Navigator.goTo("show-bookings"));

        root.getChildren().setAll(backButton, form);
    }

    private HBox buildHeader() {
        return buildHeader(null);
    }

        // Rubrik med "+ New booking" till höger.
        private HBox buildHeader(Button copyButton) {

        // READ_ONLY får bara se rubriken
        if (!canEdit) {
                return UiKit.pageHeader(
                        language.text("bookings.title"),
                        null
                );
        }

        // ADMIN och WORKSHOP får skapa bokningar
        Button newBookingButton =
                UiKit.primaryButton("+ New booking");

        newBookingButton.textProperty().bind(
                language.text("bookings.new")
        );

        newBookingButton.setOnAction(
                event -> Navigator.goTo("create-booking")
        );

        VBox actions = new VBox(8, newBookingButton);
        actions.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        if (copyButton != null) {
                actions.getChildren().add(copyButton);
        }

        return UiKit.pageHeader(
                language.text("bookings.title"),
                actions
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