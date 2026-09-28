package com.wac.autocore.ui;

import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/**
 * Små byggklossar som alla innehållsvyer delar.
 *
 * Varför en egen klass? Om varje vy själv sätter färger och storlekar blir
 * sidorna lite olika och koden full av setStyle(...). Här skapar vi bara
 * kontrollerna och ger dem style-klasser – själva utseendet (färger,
 * rundning, typsnitt) finns på ETT ställe: styles.css, sektionen
 * "Innehållsvyer".
 *
 * Klassen är final och har bara static-metoder (som Math): man anropar
 * t.ex. UiKit.primaryButton("Save") utan att skapa något UiKit-objekt.
 */
public final class UiKit {

    // Höjder för tabellrader och tabellhuvud. Används för att räkna ut
    // hur hög tabellen ska vara så att den slutar precis efter sista raden.
    private static final double TABLE_ROW_HEIGHT = 42;
    private static final double TABLE_HEADER_HEIGHT = 40;

    // Formulären i designen är smala och centrerade
    private static final double FORM_MAX_WIDTH = 560;

    private UiKit() {
    }

    // ------------------------------------------------------------
    // Rubriker och text
    // ------------------------------------------------------------

    /** Sidhuvud: stor ljus rubrik till vänster och (valfritt, får vara null) en knapp längst till höger. */
    public static HBox pageHeader(String title, Node rightAction) {
        Label titleLabel = new Label(title);
        return buildPageHeader(titleLabel, rightAction);
    }

    /**
     * Samma sidhuvud, men rubriken är bunden till en text som kan ändras,
     * t.ex. language.text("vehicles.title"). Byter språk direkt.
     */
    public static HBox pageHeader(ObservableValue<String> title, Node rightAction) {
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(title);
        return buildPageHeader(titleLabel, rightAction);
    }

    // Bygger själva sidhuvudet. Används av båda pageHeader-varianterna ovan.
    private static HBox buildPageHeader(Label titleLabel, Node rightAction) {
        titleLabel.getStyleClass().add("page-title");

        // Tom yta som växer och trycker knappen hela vägen till höger
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, titleLabel, spacer);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);
        if (rightAction != null) {
            header.getChildren().add(rightAction);
        }
        return header;
    }

    /** Dämpad text för "inget att visa", t.ex. en tom lista. */
    public static Label emptyText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-text");
        label.setWrapText(true);
        return label;
    }

    // ------------------------------------------------------------
    // Knappar
    // ------------------------------------------------------------

    /** Lila huvudknapp, t.ex. "Create customer" eller "+ New customer". */
    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    /** Grön knapp för att slutföra något, t.ex. "Complete work order" och "Process payment". */
    public static Button successButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("success-button");
        return button;
    }

    // ------------------------------------------------------------
    // Badges (små "piller" med status)
    // ------------------------------------------------------------

    /** Liten rundad etikett. variant = "purple", "grey", "yellow", "green" eller "red". */
    public static Label badge(String text, String variant) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", "badge-" + variant);
        // Badgen får aldrig krympa – då skulle texten klippas till "BOOK…"
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    /** Samma som badge, men texten hämtas från språkfilen och byter språk direkt. */
    private static Label badgeFromKey(String key, String variant) {
        Label badge = badge("", variant);
        badge.textProperty().bind(LanguageManager.getInstance().text(key));
        return badge;
    }

    /**
     * Gör om en status från modellen till en badge med rätt text och färg,
     * så att samma status ser likadan ut på alla sidor.
     * Okänd status visas som grå badge med originaltexten.
     */
    public static Label statusBadge(String modelStatus) {
        if (modelStatus == null || modelStatus.trim().isEmpty()) {
            return badgeFromKey("badge.UNKNOWN", "grey");
        }
        // Jämför utan hänsyn till stora/små bokstäver ("Paid" = "PAID")
        String status = modelStatus.trim().toUpperCase();

        if (status.equals("BOOKED")) {
            return badgeFromKey("badge.BOOKED", "purple");
        } else if (status.equals("WORK_ORDER_CREATED")) {
            return badgeFromKey("badge.WORK_ORDER_CREATED", "grey");
        } else if (status.equals("CREATED")) {
            return badgeFromKey("badge.CREATED", "grey");
        } else if (status.equals("IN_PROGRESS")) {
            // Designen kallar "IN_PROGRESS" för STARTED
            return badgeFromKey("badge.IN_PROGRESS", "yellow");
        } else if (status.equals("COMPLETED")) {
            return badgeFromKey("badge.COMPLETED", "green");
        } else if (status.equals("PAID")) {
            return badgeFromKey("badge.PAID", "green");
        } else if (status.equals("UNPAID")) {
            return badgeFromKey("badge.UNPAID", "red");
        } else if (status.equals("SUCCESSFUL")) {
            return badgeFromKey("badge.SUCCESSFUL", "green");
        } else if (status.equals("FAILED")) {
            return badgeFromKey("badge.FAILED", "red");
        } else if (status.equals("VIP")) {
            return badgeFromKey("badge.VIP", "yellow");
        }
        return badge(modelStatus, "grey");
    }

    /**
     * Cell-fabrik för en tabellkolumn med text som ska visas som badge.
     * Exempel: statusColumn.setCellFactory(UiKit.badgeCells());
     * Tomt värde eller "—" visas som ett dämpat streck (t.ex. icke-VIP).
     */
    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> badgeCells() {
        return column -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                if (empty) {
                    setGraphic(null);
                } else if (value == null || value.trim().isEmpty() || value.equals("—")) {
                    Label dash = new Label("—");
                    dash.getStyleClass().add("cell-dash");
                    setGraphic(dash);
                } else {
                    setGraphic(statusBadge(value));
                }
            }
        };
    }

    /** Liten rund prick, t.ex. framför kanban-rubriker och mekanikernamn. variant som för badge. */
    public static Region dot(String variant) {
        Region dot = new Region();
        dot.getStyleClass().addAll("dot", "dot-" + variant);
        return dot;
    }

    // ------------------------------------------------------------
    // Formulär
    // ------------------------------------------------------------

    /**
     * Smal centrerad formulärkolumn (max 560 px) som i designen.
     * Knappar som skickas in blir automatiskt lika breda som fälten.
     * Centreringen fungerar direkt i innehållsytan; ligger den i en egen
     * VBox ska den VBoxen ha setAlignment(Pos.TOP_CENTER).
     */
    public static VBox formContainer(Node... children) {
        VBox form = new VBox(16, children);
        form.getStyleClass().add("form-container");
        form.setMaxWidth(FORM_MAX_WIDTH);
        StackPane.setAlignment(form, Pos.TOP_CENTER);

        for (Node child : children) {
            if (child instanceof Button) {
                ((Button) child).setMaxWidth(Double.MAX_VALUE);
            }
        }
        return form;
    }

    /** Ett fält med sin etikett ovanför, t.ex. formField("Name", nameField). Fältet fyller bredden. */
    public static VBox formField(String label, Node field) {
        Label labelNode = new Label(label);
        return buildFormField(labelNode, field);
    }

    /**
     * Samma fält, men etiketten är bunden till en text som kan ändras,
     * t.ex. language.text("customers.name"). Byter språk direkt.
     */
    public static VBox formField(ObservableValue<String> label, Node field) {
        Label labelNode = new Label();
        labelNode.textProperty().bind(label);
        return buildFormField(labelNode, field);
    }

    // Bygger själva fältet. Används av båda formField-varianterna ovan.
    private static VBox buildFormField(Label labelNode, Node field) {
        labelNode.getStyleClass().add("form-label");

        if (field instanceof Region) {
            ((Region) field).setMaxWidth(Double.MAX_VALUE);
        }

        VBox box = new VBox(6, labelNode, field);
        box.getStyleClass().add("form-field");
        return box;
    }

    /** Lägger flera formField bredvid varandra med lika bredd (t.ex. Brand + Model). */
    public static HBox formRow(Node... fields) {
        HBox row = new HBox(14, fields);
        for (Node field : fields) {
            HBox.setHgrow(field, Priority.ALWAYS);
            if (field instanceof Region) {
                // prefWidth 0 + hgrow: alla fält börjar lika smala och får sedan
                // lika mycket av bredden. Annars blir t.ex. en DatePicker smalare
                // än en ComboBox bredvid, eftersom de "vill" olika bredd.
                ((Region) field).setPrefWidth(0);
                ((Region) field).setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    /**
     * Visar combo-boxens prompt-text (t.ex. "Select vehicle") i dämpad färg
     * när inget är valt – även efter att formuläret tömts med setValue(null).
     *
     * Varför? JavaFX tappar annars prompt-texten efter setValue(null), och
     * prompten ritas i samma ljusa färg som ett riktigt val. Den egna
     * "knappcellen" skriver ut prompten och sätter klassen combo-prompt
     * (se styles.css). Valda värden visas med combo-boxens converter.
     */
    public static <T> void keepPromptWhenCleared(ComboBox<T> comboBox) {
        comboBox.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("combo-prompt");
                if (empty || item == null) {
                    setText(comboBox.getPromptText());
                    getStyleClass().add("combo-prompt");
                } else {
                    setText(comboBox.getConverter().toString(item));
                }
            }
        });
    }

    /** Inramad ruta för en lista med CheckBoxar, t.ex. tjänster i "Create work order". */
    public static VBox checklistBox(Node... checkBoxes) {
        VBox box = new VBox(12, checkBoxes);
        box.getStyleClass().add("checklist-box");
        return box;
    }

    /** "Total"-rutan i fakturaformuläret: text till vänster, värdet (som du uppdaterar själv) till höger. */
    public static HBox totalBox(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.getStyleClass().add("total-label");
        valueLabel.getStyleClass().add("total-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(label, spacer, valueLabel);
        box.getStyleClass().add("total-box");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ------------------------------------------------------------
    // Återkoppling till användaren (fel / lyckat / info)
    // ------------------------------------------------------------

    /** Tom etikett för meddelanden. Fyll den med showError/showSuccess/showInfo. */
    public static Label feedbackLabel() {
        Label label = new Label();
        label.getStyleClass().add("feedback-text");
        label.setWrapText(true);
        return label;
    }

    /** Visar ett felmeddelande i rött. */
    public static void showError(Label label, String message) {
        showFeedback(label, message, "error-text");
    }

    /** Visar ett lyckat-meddelande i grönt. */
    public static void showSuccess(Label label, String message) {
        showFeedback(label, message, "success-text");
    }

    /** Visar neutral information i dämpad färg. */
    public static void showInfo(Label label, String message) {
        showFeedback(label, message, "info-text");
    }

    // Samma etikett kan visa fel ena gången och lyckat nästa,
    // därför tar vi bort den gamla färgklassen innan vi lägger till den nya.
    private static void showFeedback(Label label, String message, String styleClass) {
        label.getStyleClass().removeAll("error-text", "success-text", "info-text");
        label.getStyleClass().add(styleClass);
        label.setText(message);
    }

    // ------------------------------------------------------------
    // Kort
    // ------------------------------------------------------------

    /** Ljusare ruta med rundade hörn och tunn ram. */
    public static VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.getStyleClass().add("card");
        return card;
    }

    /** Klickbart kort i en lista (vänsterspalten i "Start work order"). */
    public static VBox selectCard(Node... children) {
        VBox card = new VBox(6, children);
        card.getStyleClass().add("select-card");
        return card;
    }

    /** Markerar/avmarkerar ett selectCard som valt (lila ram och bakgrund). */
    public static void setSelected(Node selectCard, boolean selected) {
        selectCard.getStyleClass().remove("select-card-active");
        if (selected) {
            selectCard.getStyleClass().add("select-card-active");
        }
    }

    /**
     * En kanban-kolumn: prick + versal rubrik överst. Lägg sedan till
     * korten med column.getChildren().add(...) (klass "kanban-card").
     */
    public static VBox kanbanColumn(String title, String dotVariant) {
        Label heading = new Label(title.toUpperCase());
        return buildKanbanColumn(heading, dotVariant);
    }

    /**
     * Samma kolumn, men rubriken är bunden till en text som kan ändras,
     * t.ex. language.text("badge.CREATED"). Texten visas som den är,
     * så nyckeln ska redan vara i versaler.
     */
    public static VBox kanbanColumn(ObservableValue<String> title, String dotVariant) {
        Label heading = new Label();
        heading.textProperty().bind(title);
        return buildKanbanColumn(heading, dotVariant);
    }

    // Bygger själva kolumnen. Används av båda kanbanColumn-varianterna ovan.
    private static VBox buildKanbanColumn(Label heading, String dotVariant) {
        heading.getStyleClass().add("kanban-header");

        HBox header = new HBox(8, dot(dotVariant), heading);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox column = new VBox(12, header);
        column.getStyleClass().add("kanban-column");
        // Alla kolumner får lika stor del av bredden
        column.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(column, Priority.ALWAYS);
        column.setPrefWidth(0);
        return column;
    }

    // ------------------------------------------------------------
    // Tabeller
    // ------------------------------------------------------------

    /**
     * Ger en TableView designens utseende: rundad ram, mörkt huvud,
     * kolumner som fyller bredden och en dämpad placeholder-text.
     * Tabellen blir exakt så hög som raderna (inga tomma rader under
     * sista raden); långa listor scrollar med hela sidan.
     * Kolumnklasser: column.getStyleClass().add("cell-muted" / "cell-strong" / "cell-right").
     */
    public static void styleTable(TableView<?> table) {
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(TABLE_ROW_HEIGHT);

        Node placeholder = table.getPlaceholder();
        if (placeholder == null) {
            table.setPlaceholder(emptyText("Nothing to show yet."));
        } else {
            placeholder.getStyleClass().add("empty-text");
        }

        fitHeightToRows(table);
    }

    // Räknar om tabellens höjd varje gång listan ändras
    // (eller byts ut med setItems).
    private static <S> void fitHeightToRows(TableView<S> table) {
        InvalidationListener update = observable -> updateTableHeight(table);

        if (table.getItems() != null) {
            table.getItems().addListener(update);
        }
        table.itemsProperty().addListener((observable, oldItems, newItems) -> {
            if (oldItems != null) {
                oldItems.removeListener(update);
            }
            if (newItems != null) {
                newItems.addListener(update);
            }
            updateTableHeight(table);
        });
        updateTableHeight(table);
    }

    private static void updateTableHeight(TableView<?> table) {
        ObservableList<?> items = table.getItems();
        int rows = items == null ? 0 : items.size();
        // En tom tabell får plats för två rader så att placeholder-texten syns
        int visibleRows = Math.max(rows, 2);
        // + 4 = ram och inre marginal (1 px runt om) plus lite luft
        double height = TABLE_HEADER_HEIGHT + visibleRows * TABLE_ROW_HEIGHT + 4;

        table.setMinHeight(height);
        table.setPrefHeight(height);
        table.setMaxHeight(height);
    }
}
