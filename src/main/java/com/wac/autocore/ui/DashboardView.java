package com.wac.autocore.ui;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Startsidan i appen. Visar en överblick över verkstaden:
 * nyckeltal, veckans arbetsordrar, senaste arbetsordrar och en kanban-tavla.
 * Sidan börjar direkt med nyckeltalen – ingen sökruta eller inloggad användare,
 * eftersom projektet varken har sökfunktion eller inloggning.
 *
 * All data läses från Database-klassens listor. När appen har kontakt med
 * MySQL har AutoCoreApp redan fyllt listorna med sparad data, annars
 * innehåller de exempeldata – dashboarden fungerar alltså i båda fallen.
 */
public class DashboardView extends VBox {

    private final LanguageManager language = LanguageManager.getInstance();

    // Höjden på själva stapelytan i diagrammet (utan dagnamn)
    private static final double CHART_HEIGHT = 150;
    private static final double MIN_BAR_HEIGHT = 4;
    private static final int RECENT_LIMIT = 4;

    public DashboardView() {
        // Avstånd i pixlar mellan raderna
        setSpacing(20);

        getChildren().addAll(
                createStatRow(),
                createChartRow(),
                createKanbanRow()
        );
    }


    // Rad 1: fyra nyckeltal


    private HBox createStatRow() {
        // Aktiva = allt som inte är klart (CONFIRM eller IN_PROGRESS)
        long activeCount = Database.getWorkOrders().stream()
                .filter(wo -> wo.getStatus() != WorkOrderStatus.COMPLETED)
                .count();

        // Summerar fakturor som skapats i innevarande månad
        YearMonth thisMonth = YearMonth.now();
        double revenue = Database.getInvoices().stream()
                .filter(inv -> inv.getInvoiceDate() != null
                        && YearMonth.from(inv.getInvoiceDate()).equals(thisMonth))
                .mapToDouble(inv -> inv.getTotalAmount())
                .sum();

        int customerCount = Database.getCustomers().size();

        // Bokningar som ännu inte fått en arbetsorder
        long pendingCount = Database.getBookings().stream()
                .filter(b -> "BOOKED".equals(b.getStatus()))
                .count();

        VBox activeCard = createStatCard(language.text("dashboard.activeTitle"),
                String.valueOf(activeCount), language.text("dashboard.activeSubtitle"));
        // Månadens namn läggs till efter texten, t.ex. "Fakturerat i september"
        VBox revenueCard = createStatCard(language.text("dashboard.revenueTitle"),
                formatSek(revenue), language.text("dashboard.revenueSubtitle").concat(" " + monthName(thisMonth)));
        VBox customersCard = createStatCard(language.text("dashboard.customersTitle"),
                String.valueOf(customerCount), language.text("dashboard.customersSubtitle"));
        VBox pendingCard = createStatCard(language.text("dashboard.pendingTitle"),
                String.valueOf(pendingCount), language.text("dashboard.pendingSubtitle"));

        // Värdet i sista kortet ska synas i accentfärg (som i designen)
        pendingCard.getChildren().get(1).getStyleClass().add("stat-value-accent");

        return new HBox(20, activeCard, revenueCard, customersCard, pendingCard);
    }

    // Titel och undertitel är bundna till språkfilen, värdet är en vanlig siffra
    private VBox createStatCard(ObservableValue<String> title, String value, ObservableValue<String> subtitle) {
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        Label subtitleLabel = new Label();
        subtitleLabel.textProperty().bind(subtitle);
        subtitleLabel.getStyleClass().add("stat-subtitle");

        VBox card = new VBox(6, titleLabel, valueLabel, subtitleLabel);
        card.getStyleClass().add("card");
        makeEqualWidth(card);
        return card;
    }


    // Rad 2: veckodiagram (ca 60 %) + senaste arbetsordrar (ca 40 %)


    private GridPane createChartRow() {
        // GridPane med procentkolumner ger ett fast förhållande 60/40,
        // vilket en HBox inte klarar lika enkelt.
        GridPane row = new GridPane();
        row.setHgap(20);

        ColumnConstraints chartColumn = new ColumnConstraints();
        chartColumn.setPercentWidth(60);
        ColumnConstraints recentColumn = new ColumnConstraints();
        recentColumn.setPercentWidth(40);
        row.getColumnConstraints().addAll(chartColumn, recentColumn);

        VBox chartCard = createWeeklyChart();
        VBox recentCard = createRecentWorkOrders();
        chartCard.setMaxHeight(Double.MAX_VALUE);
        recentCard.setMaxHeight(Double.MAX_VALUE);

        row.add(chartCard, 0, 0);
        row.add(recentCard, 1, 0);
        return row;
    }

    private VBox createWeeklyChart() {
        Label title = new Label();
        title.textProperty().bind(language.text("dashboard.weeklyTitle"));
        title.getStyleClass().add("card-title");

        Label hint = new Label();
        hint.textProperty().bind(language.text("dashboard.weeklyHint"));
        hint.getStyleClass().add("card-hint");

        // Räknar arbetsordrar per veckodag (index 0 = måndag)
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        int[] counts = new int[7];
        for (WorkOrder wo : Database.getWorkOrders()) {
            // WorkOrder saknar eget datum, så vi använder bokningens datum
            Booking booking = findBooking(wo.getBookingId());
            if (booking == null || booking.getDate() == null) {
                continue;
            }
            int dayIndex = (int) (booking.getDate().toEpochDay() - monday.toEpochDay());
            if (dayIndex >= 0 && dayIndex < 7) {
                counts[dayIndex]++;
            }
        }

        int max = 0;
        for (int count : counts) {
            max = Math.max(max, count);
        }

        HBox bars = new HBox(8);
        bars.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < 7; i++) {
            bars.getChildren().add(createBarColumn(monday.plusDays(i).getDayOfWeek(), counts[i], max));
        }

        // En tom Region som växer skjuter ner staplarna till kortets botten
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(4, title, hint, spacer, bars);
        card.getStyleClass().add("card");
        // Fast minsta höjd så att kortet ser likadant ut även utan data
        card.setMinHeight(270);
        return card;
    }

    private VBox createBarColumn(DayOfWeek day, int count, int max) {
        // Skalar stapeln mot veckans största värde, men visar alltid en liten stump
        double height = max == 0 ? MIN_BAR_HEIGHT
                : Math.max(MIN_BAR_HEIGHT, CHART_HEIGHT * count / max);

        Region bar = new Region();
        bar.getStyleClass().add("chart-bar");
        bar.setPrefSize(30, height);
        bar.setMinHeight(height);
        bar.setMaxSize(30, height);

        Label valueLabel = new Label(count > 0 ? String.valueOf(count) : "");
        valueLabel.getStyleClass().add("chart-value");

        // Dagens kortnamn på valt språk, t.ex. "mån" eller "Mon"
        Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, currentLocale()));
        dayLabel.getStyleClass().add("chart-day");

        VBox column = new VBox(4, valueLabel, bar, dayLabel);
        column.setAlignment(Pos.BOTTOM_CENTER);
        makeEqualWidth(column);
        return column;
    }

    private VBox createRecentWorkOrders() {
        Label title = new Label();
        title.textProperty().bind(language.text("dashboard.recentTitle"));
        title.getStyleClass().add("card-title");

        VBox card = new VBox(6, title);
        card.getStyleClass().add("card");

        // Senaste = högst id först
        List<WorkOrder> recent = new ArrayList<WorkOrder>(Database.getWorkOrders());
        Collections.sort(recent, new Comparator<WorkOrder>() {
            @Override
            public int compare(WorkOrder a, WorkOrder b) {
                return Integer.compare(b.getId(), a.getId());
            }
        });

        if (recent.isEmpty()) {
            card.getChildren().add(createEmptyText("dashboard.noWorkOrders"));
            return card;
        }

        for (int i = 0; i < recent.size() && i < RECENT_LIMIT; i++) {
            card.getChildren().add(createRecentRow(recent.get(i)));
        }
        return card;
    }

    private HBox createRecentRow(WorkOrder wo) {
        Vehicle vehicle = findVehicleForWorkOrder(wo);

        Label heading = new Label(workOrderCode(wo) + " · " + vehicleText(vehicle));
        heading.getStyleClass().add("row-title");
        // Långa modellnamn (t.ex. "Volkswagen Passat") bryts till en ny rad
        // istället för att klippas till "Pa…" – regnumret ska alltid gå att läsa.
        heading.setWrapText(true);

        Label people = new Label(customerName(vehicle) + " · " + mechanicName(wo.getMechanicId()));
        people.getStyleClass().add("row-subtitle");

        VBox text = new VBox(2, heading, people);
        // Texten får krympa (och bryta rad) så att badgen alltid syns
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(10, text, createStatusBadge(wo.getStatus()));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("recent-row");
        return row;
    }

    private Label createStatusBadge(WorkOrderStatus status) {
        Label badge = new Label();
        badge.getStyleClass().add("badge");
        // Badgen får aldrig krympa – då skulle texten klippas till "STAR…"
        badge.setMinWidth(Region.USE_PREF_SIZE);

        if (status == WorkOrderStatus.IN_PROGRESS) {
            badge.textProperty().bind(language.text("badge.IN_PROGRESS"));
            badge.getStyleClass().add("badge-started");
        } else if (status == WorkOrderStatus.COMPLETED) {
            badge.textProperty().bind(language.text("badge.COMPLETED"));
            badge.getStyleClass().add("badge-completed");
        } else {
            badge.textProperty().bind(language.text("badge.CREATED"));
            badge.getStyleClass().add("badge-created");
        }
        return badge;
    }


    // Rad 3: kanban med tre kolumner


    private HBox createKanbanRow() {
        return new HBox(20,
                createKanbanColumn("dashboard.pending", WorkOrderStatus.CONFIRMED),
                createKanbanColumn("dashboard.inProgress", WorkOrderStatus.IN_PROGRESS),
                createKanbanColumn("dashboard.readyForPickup", WorkOrderStatus.COMPLETED));
    }

    private VBox createKanbanColumn(String titleKey, WorkOrderStatus status) {
        List<WorkOrder> orders = new ArrayList<WorkOrder>();
        for (WorkOrder wo : Database.getWorkOrders()) {
            if (wo.getStatus() == status) {
                orders.add(wo);
            }
        }

        // Rubrik + antal, t.ex. "Pågående (1)". concat gör att antalet
        // följer med när texten byter språk.
        Label heading = new Label();
        heading.textProperty().bind(language.text(titleKey).concat(" (" + orders.size() + ")"));
        heading.getStyleClass().add("card-title");

        VBox column = new VBox(10, heading);
        column.getStyleClass().add("card");
        makeEqualWidth(column);

        if (orders.isEmpty()) {
            column.getChildren().add(createEmptyText("dashboard.nothingHere"));
        }
        for (WorkOrder wo : orders) {
            column.getChildren().add(createKanbanItem(wo));
        }
        return column;
    }

    private VBox createKanbanItem(WorkOrder wo) {
        Vehicle vehicle = findVehicleForWorkOrder(wo);

        Label title = new Label(workOrderCode(wo) + " · " + workDescription(wo));
        title.getStyleClass().add("kanban-title");

        Label customer = new Label(customerName(vehicle));
        customer.getStyleClass().add("kanban-customer");

        Label vehicleLabel = new Label(vehicleText(vehicle));
        vehicleLabel.getStyleClass().add("kanban-vehicle");

        VBox item = new VBox(2, title, customer, vehicleLabel);
        item.getStyleClass().add("kanban-item");
        return item;
    }


    // Små hjälpmetoder


    /** Får flera kort i samma HBox att dela lika på bredden. */
    private void makeEqualWidth(Region region) {
        HBox.setHgrow(region, Priority.ALWAYS);
        region.setMaxWidth(Double.MAX_VALUE);
        // Prefbredd 0 gör att alla startar lika, sedan fördelar Hgrow resten
        region.setPrefWidth(0);
    }

    // Dämpad text som byter språk direkt, t.ex. "Inget här just nu"
    private Label createEmptyText(String key) {
        Label label = new Label();
        label.textProperty().bind(language.text(key));
        label.getStyleClass().add("empty-text");
        return label;
    }

    // Java-språket som hör till valt språk ("sv" eller "en"), för månader och veckodagar
    private Locale currentLocale() {
        return new Locale(language.text("language.code").get());
    }

    private String formatSek(double amount) {
        // Svensk Locale ger mellanslag som tusentalsavgränsare, t.ex. "12 345 SEK"
        return String.format(new Locale("sv", "SE"), "%,.0f SEK", amount);
    }

    private String monthName(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, currentLocale());
    }

    private String workOrderCode(WorkOrder wo) {
        return "WO-" + wo.getId();
    }

    /** Tjänsternas namn om de finns, annars bokningens beskrivning. */
    private String workDescription(WorkOrder wo) {
        List<String> names = new ArrayList<String>();
        for (Integer serviceId : wo.getServiceItemIds()) {
            for (ServiceItem item : Database.getServiceItems()) {
                if (item.getId() == serviceId) {
                    names.add(item.getName());
                }
            }
        }
        if (!names.isEmpty()) {
            return String.join(", ", names);
        }
        Booking booking = findBooking(wo.getBookingId());
        return booking != null ? booking.getDescription() : language.text("dashboard.workOrder").get();
    }

    private Booking findBooking(int bookingId) {
        for (Booking booking : Database.getBookings()) {
            if (booking.getId() == bookingId) {
                return booking;
            }
        }
        return null;
    }

    private Vehicle findVehicleForWorkOrder(WorkOrder wo) {
        Booking booking = findBooking(wo.getBookingId());
        if (booking == null) {
            return null;
        }
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == booking.getVehicleId()) {
                return vehicle;
            }
        }
        return null;
    }

    private String vehicleText(Vehicle vehicle) {
        if (vehicle == null) {
            return language.text("common.unknownVehicle").get();
        }
        return vehicle.getRegistrationNumber() + " · " + vehicle.getBrand() + " " + vehicle.getModel();
    }

    private String customerName(Vehicle vehicle) {
        if (vehicle != null) {
            for (Customer customer : Database.getCustomers()) {
                if (customer.getId() == vehicle.getCustomerId()) {
                    return customer.getName();
                }
            }
        }
        return language.text("common.unknownCustomer").get();
    }

    private String mechanicName(int mechanicId) {
        for (Mechanic mechanic : Database.getMechanics()) {
            if (mechanic.getId() == mechanicId) {
                return mechanic.getName();
            }
        }
        return language.text("bookings.notAssigned").get();
    }
}