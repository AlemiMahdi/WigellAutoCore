package com.wac.autocore.ui.views;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.security.actions.SecuredMechanicActions;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MechanicScheduleView {

        private static final Logger LOGGER =
        LoggerFactory.getLogger(MechanicScheduleView.class);
        private final LanguageManager language = LanguageManager.getInstance();
        private final SecuredMechanicActions securedMechanicActions;

        public MechanicScheduleView(
                SecuredMechanicActions securedMechanicActions) {
        this.securedMechanicActions = securedMechanicActions;
        }

    public VBox getView() {

        // Titel
        Node title = UiKit.pageHeader(
                language.text("schedule.title"),
                null
        );

        // Rullista
        ComboBox<Mechanic> mechanicCombo = new ComboBox<>();
        mechanicCombo.getItems().addAll(securedMechanicActions.getLoadedMechanics());
        mechanicCombo.promptTextProperty().bind(
                language.text("schedule.select")
        );

        Label countLabel = new Label();

        // Tabell
        TableView<Booking> table = new TableView<>();

        TableColumn<Booking, Integer> idCol =
                new TableColumn<>("ID");
        idCol.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        TableColumn<Booking, LocalDate> dateCol =
                new TableColumn<>("Date");
        dateCol.setCellValueFactory(
                new PropertyValueFactory<>("date")
        );

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

        TableColumn<Booking, Integer> vehicleCol =
                new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(
                new PropertyValueFactory<>("vehicleId")
        );

        TableColumn<Booking, String> descCol =
                new TableColumn<>("Description");
        descCol.setCellValueFactory(
                new PropertyValueFactory<>("description")
        );

        TableColumn<Booking, String> statusCol =
                new TableColumn<>("Status");
        statusCol.setCellValueFactory(
                new PropertyValueFactory<>("status")
        );

        idCol.textProperty().bind(
                language.text("schedule.id")
        );
        dateCol.textProperty().bind(
                language.text("schedule.date")
        );
        startTimeCol.textProperty().bind(
                language.text("schedule.startTime")
        );
        durationCol.textProperty().bind(
                language.text("schedule.duration")
        );
        vehicleCol.textProperty().bind(
                language.text("schedule.vehicle")
        );
        descCol.textProperty().bind(
                language.text("schedule.description")
        );
        statusCol.textProperty().bind(
                language.text("schedule.status")
        );

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
        selectLabel.textProperty().bind(
                language.text("schedule.select")
        );

        table.setPlaceholder(selectLabel);
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        mechanicCombo.setOnAction(actionEvent -> {

            Mechanic selected = mechanicCombo.getValue();

            if (selected == null) {
                return;
            }

            List<Booking> result;

            try {
                result = securedMechanicActions.getSchedule(selected.getId());
            } catch (RuntimeException exception) {
                
                LOGGER.error(
                        "Could not load schedule for mechanic {}",
                        selected.getId(),
                        exception
                );
                table.getItems().clear();
                countLabel.setText("");

                Label errorLabel = UiKit.feedbackLabel();
                UiKit.showError(errorLabel, "");
                errorLabel.textProperty().bind(
                        language.text("schedule.loadError")
                );
                errorLabel.setWrapText(true);
                table.setPlaceholder(errorLabel);
                return;
            }

            table.setItems(
                    FXCollections.observableArrayList(result)
            );

            {
                Label emptyLabel = UiKit.emptyText("");
                emptyLabel.textProperty().bind(
                        language.text("schedule.empty")
                );
                table.setPlaceholder(emptyLabel);
            }

            countLabel.setText(
                    language.text("schedule.count").get()
                            + " "
                            + result.size()
            );
        });

        return new VBox(
                10,
                title,
                mechanicCombo,
                table,
                countLabel
        );
    }
}