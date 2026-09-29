package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Visar befintliga tjänster i en tabell och låter användaren ändra tidsåtgången.
public class ServiceView extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();
    private final TableView<ServiceItem> table = new TableView<ServiceItem>();
    private final TextField durationField = new TextField();
    private final Label feedbackLabel = UiKit.feedbackLabel();
    private final ServiceItemRepository repository = new ServiceItemRepository();

    public ServiceView() {
        setSpacing(20);

        buildTable();

        getChildren().addAll(
                UiKit.pageHeader(language.text("services.title"), null),
                table,
                buildEditCard()
        );
    }

    // Tabell: Service (fet) / Description (dämpad) / Price / Est. time (högerställda)
    private void buildTable() {
        TableColumn<ServiceItem, String> nameColumn = new TableColumn<ServiceItem, String>("Service");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.getStyleClass().add("cell-strong");

        TableColumn<ServiceItem, String> descriptionColumn = new TableColumn<ServiceItem, String>("Description");
        descriptionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        descriptionColumn.getStyleClass().add("cell-muted");

        TableColumn<ServiceItem, String> priceColumn = new TableColumn<ServiceItem, String>("Price");
        priceColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(formatPrice(cell.getValue().getPrice())));
        priceColumn.getStyleClass().add("cell-right");

        TableColumn<ServiceItem, String> timeColumn = new TableColumn<ServiceItem, String>("Est. time");
        timeColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getEstimatedMinutes() + " min"));
        timeColumn.getStyleClass().addAll("cell-right", "cell-muted");

        nameColumn.textProperty().bind(language.text("services.name"));
        descriptionColumn.textProperty().bind(language.text("services.description"));
        priceColumn.textProperty().bind(language.text("services.price"));
        timeColumn.textProperty().bind(language.text("services.time"));

        // Kolumnbredder i procent. CONSTRAINED_RESIZE_POLICY fördelar bredden
        // i proportion till maxWidth, så stora maxvärden fungerar som "vikter".
        // Beskrivningen får mest plats så att den inte klipps med "...".
        setColumnWeight(nameColumn, 24);
        setColumnWeight(descriptionColumn, 46);
        setColumnWeight(priceColumn, 16);
        setColumnWeight(timeColumn, 14);

        table.getColumns().add(nameColumn);
        table.getColumns().add(descriptionColumn);
        table.getColumns().add(priceColumn);
        table.getColumns().add(timeColumn);

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(language.text("services.empty"));
        table.setPlaceholder(emptyLabel);
        UiKit.styleTable(table);

        // Hämtar aktuella tjänster varje gång vyn öppnas
        table.setItems(FXCollections.observableArrayList(Database.getServiceItems()));

        // Visar tidsåtgången för tjänsten som användaren väljer.
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> {
                    feedbackLabel.setText("");

                    if (selected == null) {
                        durationField.clear();
                    } else {
                        durationField.setText(String.valueOf(selected.getEstimatedMinutes()));
                    }
                }
        );
    }

    // Kort under tabellen där man ändrar "Est. time" för vald tjänst.
    private VBox buildEditCard() {
        Label heading = new Label("Change estimated time");
        heading.textProperty().bind(language.text("services.editTitle"));
        heading.getStyleClass().add("card-heading");

        Label hint = new Label("Select a service in the table and enter the new time in minutes.");
        hint.textProperty().bind(language.text("services.prompt"));
        hint.getStyleClass().add("detail-text");
        hint.setWrapText(true);

        durationField.promptTextProperty().bind(language.text("services.minutesPrompt"));
        durationField.setPrefColumnCount(10);

        Button saveButton = UiKit.primaryButton("Save duration");
        saveButton.textProperty().bind(language.text("services.save"));
        saveButton.setOnAction(event -> saveDuration());

        HBox inputRow = new HBox(12, durationField, saveButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(durationField, Priority.NEVER);

        // Tomt meddelande ska inte lämna ett hål längst ner i kortet
        feedbackLabel.managedProperty().bind(feedbackLabel.textProperty().isNotEmpty());
        feedbackLabel.visibleProperty().bind(feedbackLabel.textProperty().isNotEmpty());

        VBox card = UiKit.card(heading, hint, inputRow, feedbackLabel);
        card.getStyleClass().add("detail-card");
        return card;
    }

    // Samma validering och sparning som tidigare, via ServiceItemRepository.
    private void saveDuration() {
        ServiceItem selected = table.getSelectionModel().getSelectedItem();

        if (selected == null) {
            UiKit.showError(feedbackLabel, language.text("services.select").get());
            return;
        }

        int minutes;

        try {
            minutes = Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException exception) {
            UiKit.showError(feedbackLabel, language.text("services.invalidNumber").get());
            return;
        }

        if (minutes <= 0) {
            UiKit.showError(feedbackLabel, language.text("services.invalidDuration").get());
            return;
        }

        // Behåller det gamla värdet om sparandet misslyckas.
        int previousMinutes = selected.getEstimatedMinutes();
        selected.setEstimatedMinutes(minutes);

        try {
            repository.save(selected);
        } catch (RuntimeException exception) {
            selected.setEstimatedMinutes(previousMinutes);
            table.refresh();

            UiKit.showError(feedbackLabel, language.text("services.error").get());
            exception.printStackTrace();
            return;
        }

        table.refresh();
        UiKit.showSuccess(feedbackLabel, language.text("services.saved").get());
    }

    private void setColumnWeight(TableColumn<ServiceItem, String> column, int percent) {
        column.setMaxWidth(percent * 100000.0);
    }

    // 1450.0 -> "1 450 SEK" (mellanslag som tusentalsavgränsare, som i designen)
    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(price) + " SEK";
    }
}
