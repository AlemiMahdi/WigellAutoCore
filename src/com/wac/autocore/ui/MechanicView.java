package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

// Visar systemets mekaniker som kort, två per rad.
public class MechanicView extends VBox {

    private static final int CARDS_PER_ROW = 2;
    private final LanguageManager language = LanguageManager.getInstance();

    public MechanicView() {
        setSpacing(20);

        // Hämtar aktuella mekaniker när vyn öppnas
        List<Mechanic> mechanics = Database.getMechanics();

        if (mechanics.isEmpty()) {
            Label emptyLabel = UiKit.emptyText("");
            emptyLabel.textProperty().bind(language.text("mechanics.empty"));
            getChildren().addAll(
                    UiKit.pageHeader(language.text("mechanics.title"), null),
                    emptyLabel);
            return;
        }

        getChildren().addAll(UiKit.pageHeader(language.text("mechanics.title"), null), buildCardGrid(mechanics));
    }

    // GridPane med två lika breda kolumner – korten fyller bredden och radbryts jämnt.
    private GridPane buildCardGrid(List<Mechanic> mechanics) {
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        for (int i = 0; i < CARDS_PER_ROW; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / CARDS_PER_ROW);
            column.setHgrow(Priority.ALWAYS);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }

        for (int i = 0; i < mechanics.size(); i++) {
            VBox card = createMechanicCard(mechanics.get(i));
            // Kolumn = i % 2, rad = i / 2 → 0,0  1,0  0,1  1,1 ...
            grid.add(card, i % CARDS_PER_ROW, i / CARDS_PER_ROW);
        }
        return grid;
    }

    // Ett kort: prick + namn, specialisering, telefon och tillgänglighet.
    private VBox createMechanicCard(Mechanic mechanic) {
        boolean available = mechanic.isAvailable();

        Label nameLabel = new Label(mechanic.getName());
        nameLabel.getStyleClass().add("mechanic-name");

        // Grön prick = ledig, grå prick = upptagen med en arbetsorder
        HBox nameRow = new HBox(10, UiKit.dot(available ? "green" : "grey"), nameLabel);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label roleLabel = new Label(mechanic.getSpecialization());
        roleLabel.getStyleClass().add("mechanic-role");

        Label phoneLabel = new Label(mechanic.getPhone());
        phoneLabel.getStyleClass().add("mechanic-phone");

        Label availabilityLabel = new Label();
        availabilityLabel.textProperty().bind(language.text(available ? "mechanics.available" : "mechanics.unavailable"));
        availabilityLabel.getStyleClass().add(available ? "availability-available" : "availability-off");

        VBox card = new VBox(nameRow, roleLabel, phoneLabel, availabilityLabel);
        card.getStyleClass().add("mechanic-card");
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }
}
