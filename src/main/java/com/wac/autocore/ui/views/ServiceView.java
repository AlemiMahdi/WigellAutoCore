package com.wac.autocore.ui.views;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.security.actions.SecuredServiceItemActions;
import com.wac.autocore.ui.ErrorHandler;
import org.springframework.security.access.AccessDeniedException;
import com.wac.autocore.ui.UiKit;
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

// Visar befintliga tjänster i en tabell och låter användaren ändra tidsåtgången.
public class ServiceView extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();
    private final TableView<ServiceItem> table = new TableView<>();
    private final TextField durationField = new TextField();
    private final Label feedbackLabel = UiKit.feedbackLabel();
    private final TextField priceField = new TextField();
    private final Label priceFeedbackLabel = UiKit.feedbackLabel();
    private final SecuredServiceItemActions securedServiceItemActions;

    public ServiceView(
            SecuredServiceItemActions securedServiceItemActions,
            boolean canEditDuration,
            boolean canEditPrice) {

        this.securedServiceItemActions = securedServiceItemActions;

        setSpacing(20);
        buildTable();

        getChildren().addAll(
                UiKit.pageHeader(language.text("services.title"), null),
                table
        );

        if (canEditDuration) {
            getChildren().add(buildEditCard());
        }

        if (canEditPrice) {
            getChildren().add(buildPriceCard());
        }
    }

    // Tabell: Service (fet) / Description (dämpad) / Price / Est. time (högerställda)
    private void buildTable() {
        TableColumn<ServiceItem, String> nameColumn = new TableColumn<>("Service");
        nameColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.getStyleClass().add("cell-strong");

        TableColumn<ServiceItem, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        descriptionColumn.getStyleClass().add("cell-muted");

        TableColumn<ServiceItem, String> priceColumn = new TableColumn<>("Price");
        priceColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(ViewHelpMethod.formatPrice(cell.getValue().getPrice())));
        priceColumn.getStyleClass().add("cell-right");

        TableColumn<ServiceItem, String> timeColumn = new TableColumn<>("Est. time");
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
                    priceFeedbackLabel.setText("");

                    if (selected == null) {
                        durationField.clear();
                        priceField.clear();

                    } else {
                        durationField.setText(String.valueOf(selected.getEstimatedMinutes()));
                        priceField.setText(String.valueOf(selected.getPrice()));
                    }
                }
        );
    }

    // Kort under tabellen där man ändrar "Est. Time" för vald tjänst.
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
        saveButton.setOnAction(event -> {
            ServiceItem selected = table.getSelectionModel().getSelectedItem();

            try {
                int minutes = Integer.parseInt(durationField.getText().trim());
                securedServiceItemActions.saveDuration(selected, minutes);
                table.refresh();
                UiKit.showSuccess(feedbackLabel, language.text("services.saved").get());
            } catch (NumberFormatException exception) {
                UiKit.showError(feedbackLabel, language.text("services.invalidNumber").get());
            } catch (RuntimeException exception) {
                table.refresh();
                ErrorHandler.show(feedbackLabel, exception, "services.error", "service " + selected.getId());
            }

        });

        return inputRow(heading, hint, durationField, feedbackLabel, saveButton);
    }

    private VBox buildPriceCard() {
        Label heading = new Label("Change price");
        heading.textProperty().bind(language.text("services.priceEditTitle"));
        heading.getStyleClass().add("card-heading");

        Label hint = new Label("Select a service in the table and enter the new price.");
        hint.textProperty().bind(language.text("services.pricePrompt"));
        hint.getStyleClass().add("detail-text");
        hint.setWrapText(true);

        priceField.promptTextProperty().bind(language.text("services.pricePlaceholder"));
        priceField.setPrefColumnCount(10);

        Button saveButton = UiKit.primaryButton("Save price");
        saveButton.textProperty().bind(language.text("services.savePrice"));
        saveButton.setOnAction(event -> {
            ServiceItem selected = table.getSelectionModel().getSelectedItem();

            try {
                double price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
                securedServiceItemActions.savePrice(selected, price);
                table.refresh();
                UiKit.showSuccess(priceFeedbackLabel, language.text("services.priceSaved").get());
            } catch (NumberFormatException exception) {
                UiKit.showError(priceFeedbackLabel, language.text("services.invalidPrice").get());
            } catch (RuntimeException exception) {
                table.refresh();
                ErrorHandler.show(priceFeedbackLabel, exception, "services.priceError", "service " + selected.getId());
            }

        });

        return inputRow(heading, hint, priceField, priceFeedbackLabel, saveButton);
    }

    private void setColumnWeight(TableColumn<ServiceItem, String> column, int percent) {
        column.setMaxWidth(percent * 100000.0);
    }

    private VBox inputRow(Label heading, Label hint, TextField field, Label feedback, Button saveButton) {
        HBox inputRow = new HBox(12, field, saveButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.NEVER);

        // Tomt meddelande ska inte lämna ett hål längst ner i kortet
        feedback.managedProperty().bind(feedback.textProperty().isNotEmpty());
        feedback.visibleProperty().bind(feedback.textProperty().isNotEmpty());

        VBox card = UiKit.card(heading, hint, inputRow, feedback);
        card.getStyleClass().add("detail-card");
        return card;
    }
}
