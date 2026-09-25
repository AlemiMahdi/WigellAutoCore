package com.wac.autocore.ui;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.WorkOrder;
import java.time.DayOfWeek;
import java.time.LocalDate;

import com.wac.autocore.data.Database;
import java.time.YearMonth;
import java.util.Locale;

public class DashboardView extends VBox {

    public DashboardView(){
        // Avstånd i pixlar mellan raderna (statRow, och senare diagram/kanban).
        setSpacing(20);

        //--- 1. Aktiva arbetsordrar ---
        long activeCount = Database.getWorkOrders().stream()
                .filter(wo -> !wo.getStatus().equals("COMPLETED"))
                .count();
        //--- 2. Intäkt denna månad ---
        double revenue = Database.getInvoices().stream()
                .filter(inv -> YearMonth.from(inv.getInvoiceDate()).equals(YearMonth.now()))
                .mapToDouble(inv -> inv.getTotalAmount())
                .sum();

        //--- 3. Antal kunder ---
        int customerCount = Database.getCustomers().size();

        //--- 4. Väntar på godkännande ---
        long pendingCount = Database.getBookings().stream()
                .filter(b -> b.getStatus().equals("BOOKED"))
                .count();

        //--- Skapar de fyra korten ---
        VBox activeCard    = createStatCard("ACTIVE WORK ORDERS", String.valueOf(activeCount), "Created or in progress");
        VBox revenueCard = createStatCard("REVENUE THIS MONTH",
                String.format(new Locale("sv", "SE"), "%,.0f SEK", revenue),
                "Invoiced this month");
        VBox customersCard = createStatCard("CUSTOMERS", String.valueOf(customerCount),
                "Registered customers");
        VBox pendingCard = createStatCard("PENDING APPROVAL", String.valueOf(pendingCount),
                "Awaiting work order");

        //--- Lägger korten bredvid varandra ---
        HBox statRow = new HBox(20, activeCard, revenueCard, customersCard, pendingCard);


        //--- Lägga radan i själva dashboarden ---
        getChildren().add(statRow);

    }
    private VBox createStatCard(String title, String value, String subtitle){

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("stat-subtitle");

        //--- Själva kortet ---
        VBox card = new VBox(6, titleLabel, valueLabel, subtitleLabel);
        card.getStyleClass().add("card");

        //--- Få korten att dela lika på bredden ---
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(0);

        return card;
    }
    private VBox createWeeklyChart(){
        
    }
}
