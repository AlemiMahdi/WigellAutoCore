package com.wac.autocore.ui;


import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import javafx.application.Platform;
import com.wac.autocore.data.HibernateUtil;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.wac.autocore.ui.views.ShowBookingsView;
import com.wac.autocore.ui.views.ShowPaymentsView;

import com.wac.autocore.ui.views.ShowWorkOrdersView;
import com.wac.autocore.ui.views.CreateBookingView;
import com.wac.autocore.ui.views.ProcessPaymentView;
import com.wac.autocore.data.Database;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class AutoCoreApp extends Application {

    private StackPane contentPane;

    // ScrollPane runt innehållsytan, så att höga vyer går att scrolla
    private ScrollPane contentScroll;

    // Headerns kategoridel: knapparna för vald kategori visas här.
    private HBox categoryArea;
    private Label categoryLabel;

    // Vi håller reda på aktiv kategori och aktiv knapp var för sig,
    // så att kategorin i sidomenyn förblir markerad när man väljer en knapp.
    private Labeled activeCategory;
    private Labeled activeButton;

    // Dashboard-rubriken i sidomenyn (markeras som aktiv vid start).
    private Label dashboardItem;

    // Sätts till false om databasen inte gick att nå vid start.
    private boolean databaseOnline = true;

    // Alla knappar, med sidnyckel (t.ex. "create-customer") som nyckel.
    // Används av Navigator så att en knapp i en vy kan "klicka" på menyn.
    private final Map<String, Button> navButtonsByKey = new HashMap<String, Button>();

    // För varje knapp: koden som visar knappens kategori i headern.
    // Behövs när Navigator byter sida, eftersom knappen då kanske
    // tillhör en annan kategori än den som visas just nu.
    private final Map<Button, Runnable> showCategoryOfButton = new HashMap<Button, Runnable>();

    @Override
    public void start(Stage primaryStage) {
        try {
            loadCustomers();
            loadVehicles();
            loadServiceItems();
            loadMechanics();
            loadBookings();
            loadWorkOrders();

        } catch (RuntimeException exception) {
            // Utan databas kör vi vidare med exempeldatan i Database,
            // så att GUI:t går att visa och testa ändå.
            exception.printStackTrace();
            databaseOnline = false;
        }

        BorderPane root = new BorderPane();

        VBox header = createHeader();
        ScrollPane menu = createMenu();

        // Område där våra olika sidor ska visas
        contentPane = new StackPane();
        contentPane.getStyleClass().add("app-content");
        contentPane.setPadding(new Insets(30));
        contentPane.setAlignment(Pos.TOP_LEFT);

        // Höga vyer (t.ex. dashboardens kanban) ska gå att scrolla till.
        // Därför ligger hela innehållsytan i en ScrollPane.
        contentScroll = createContentScroll(contentPane);

        root.setTop(header);
        root.setLeft(menu);
        root.setCenter(contentScroll);

        // Knappar inne i vyerna (t.ex. "+ New customer") byter sida via
        // Navigator. Vi gör samma sak som när man klickar i menyn.
        Navigator.setHandler(this::navigateTo);

        // Appen startar alltid på dashboarden.
        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm()
        );

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadCustomers() {
        CustomerRepository repository = new CustomerRepository();
        List<Customer> savedCustomers = repository.findAllCustomers();

        if (savedCustomers.isEmpty()) {
            // Sparar originalets exempelkunder vid första starten.
            for (Customer customer : Database.getCustomers()) {
                repository.save(customer);
            }
            return;
        }

        // Kontrollerar ID-ordningen eftersom originalet använder kundlistans storlek + 1 för att skapa nästa ID.
        for (int i = 0; i < savedCustomers.size(); i++) {
            if (savedCustomers.get(i).getId() != i + 1) {
                throw new IllegalStateException(
                        "Customer IDs must be consecutive, starting at 1."
                );
            }
        }

        // Ersätter kunderna i minnet med de sparade kunderna.
        Database.getCustomers().clear();
        Database.getCustomers().addAll(savedCustomers);
    }

    // Läser in fordon efter att kunderna har laddats.
    private void loadVehicles() {
        VehicleRepository repository = new VehicleRepository();
        List<Vehicle> savedVehicles = repository.findAllVehicles();

        boolean firstRun = savedVehicles.isEmpty();

        List<Vehicle> vehicles = firstRun
                ? Database.getVehicles()
                : savedVehicles;

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);

            // Originalet använder listans storlek + 1 för nästa ID.
            if (vehicle.getId() != i + 1) {
                throw new IllegalStateException(
                        "Vehicle IDs must be consecutive, starting at 1."
                );
            }

            // Kontrollerar att fordonets kund finns.
            boolean customerExists = Database.getCustomers().stream()
                    .anyMatch(customer ->
                            customer.getId() == vehicle.getCustomerId());

            if (!customerExists) {
                throw new IllegalStateException(
                        "Customer missing for vehicle " + vehicle.getId()
                );
            }
        }

        if (firstRun) {
            // Sparar originalets exempelfordon när tabellen är tom.
            for (Vehicle vehicle : vehicles) {
                repository.save(vehicle);
            }
        } else {
            // Återställer sparade fordon i programmets minne.
            Database.getVehicles().clear();
            Database.getVehicles().addAll(savedVehicles);
        }
    }

    private void loadServiceItems() {

        ServiceItemRepository repository =
                new ServiceItemRepository();

        List<ServiceItem> savedServiceItems =
                repository.findAllServiceItems();

        if (savedServiceItems.isEmpty()) {

            // Första starten: sparar originalets exempeldata.
            for (ServiceItem serviceItem : Database.getServiceItems()) {
                repository.save(serviceItem);
            }

            return;
        }

        // Kontrollerar att ID:n följer originalets struktur.
        for (int i = 0; i < savedServiceItems.size(); i++) {
            if (savedServiceItems.get(i).getId() != i + 1) {
                throw new IllegalStateException(
                        "Service item IDs must be consecutive, starting at 1."
                );
            }
        }

        // Ersätter minnesdatan med datan från databasen.
        Database.getServiceItems().clear();
        Database.getServiceItems().addAll(savedServiceItems);
    }


    private void loadMechanics() {

        MechanicRepository repository =
                new MechanicRepository();

        List<Mechanic> savedMechanics =
                repository.findAllMechanics();

        if (savedMechanics.isEmpty()) {

            // Första starten: sparar originalets exempeldata.
            for (Mechanic mechanic : Database.getMechanics()) {
                repository.save(mechanic);
            }

            return;
        }

        // Kontrollerar ID-ordningen.
        for (int i = 0; i < savedMechanics.size(); i++) {
            if (savedMechanics.get(i).getId() != i + 1) {
                throw new IllegalStateException(
                        "Mechanic IDs must be consecutive, starting at 1."
                );
            }
        }

        // Ersätter minnesdatan med datan från databasen.
        Database.getMechanics().clear();
        Database.getMechanics().addAll(savedMechanics);
    }

    // Läser in bokningar efter att fordonen har laddats,
    // eftersom varje bokning måste peka på ett befintligt fordon.
    private void loadBookings() {
        BookingRepository repository = new BookingRepository();
        List<Booking> savedBookings = repository.findAllBookings();

        // Tom tabell betyder att appen startas för första gången.
        boolean firstRun = savedBookings.isEmpty();

        // Första gången kontrolleras originalets exempeldata,
        // annars de bokningar som hämtats från databasen.
        List<Booking> bookings = firstRun ? Database.getBookings() : savedBookings;

        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);

            // Originalet räknar ut nästa ID som listans storlek + 1,
            // så ID:na måste vara 1, 2, 3... utan luckor.
            if (booking.getId() != i + 1) {
                throw new IllegalStateException(
                        "Booking IDs must be consecutive, starting at 1."
                );
            }

            // Kontrollerar att bokningens fordon finns.
            boolean vehicleExists = Database.getVehicles().stream()
                    .anyMatch(vehicle ->
                            vehicle.getId() == booking.getVehicleId());

            if (!vehicleExists) {
                throw new IllegalStateException(
                        "Vehicle missing for booking " + booking.getId()
                );
            }
        }

        if (firstRun) {
            // Sparar originalets exempelbokningar när tabellen är tom.
            for (Booking booking : bookings) {
                repository.save(booking);
            }
        } else {
            // Ersätter exempelbokningarna i minnet med de sparade bokningarna.
            Database.getBookings().clear();
            Database.getBookings().addAll(savedBookings);
        }
    }

    // Läser in arbetsordrar sist, eftersom de pekar på
    // bokningar, mekaniker och tjänster som måste vara laddade först.
    private void loadWorkOrders() {

        WorkOrderRepository repository = new WorkOrderRepository();
        List<WorkOrder> savedWorkOrders = repository.findAllWorkOrders();

        // Originalet har inga exempelarbetsordrar,
        // så en tom tabell betyder att det inte finns något att läsa in.
        if (savedWorkOrders.isEmpty()) {
            return;
        }

        for (int i = 0; i < savedWorkOrders.size(); i++) {
            WorkOrder workOrder = savedWorkOrders.get(i);

            // Originalet räknar ut nästa ID som listans storlek + 1,
            // så ID:na måste vara 1, 2, 3... utan luckor.
            if (workOrder.getId() != i+1) {
                throw new IllegalStateException(
                        "Work order IDs must be consecutive, starting at 1."
                );
            }
            // Kontrollerar att arbetsorderns bokning finns.
            boolean bookingExists = Database.getBookings().stream()
                    .anyMatch(booking ->
                            booking.getId() == workOrder.getBookingId());

            if (!bookingExists) {
                throw new IllegalStateException(
                        "Booking missing for work order " + workOrder.getId()
                );
            }
            // Kontrollerar att arbetsorderns mekaniker finns.
            boolean mechanicsExist = Database.getMechanics().stream()
                    .anyMatch(mechanic ->
                            mechanic.getId() == workOrder.getMechanicId());

            if (!mechanicsExist) {
                throw new IllegalStateException(
                        "Mechanic missing for work order " + workOrder.getId()
                );
            }
            // Kontrollerar att alla arbetsorderns tjänster finns.
            for (int serviceItemId : workOrder.getServiceItemIds()) {
                boolean serviceItemExists = Database.getServiceItems().stream().
                        anyMatch(serviceItem ->
                                serviceItem.getId() == serviceItemId);

                if (!serviceItemExists) {
                    throw new IllegalStateException(
                            "Service item missing for work order " + workOrder.getId()
                    );
                }
            }
        }
        // Ersätter arbetsordrarna i minnet med de sparade arbetsordrarna.
        Database.getWorkOrders().clear();
        Database.getWorkOrders().addAll(savedWorkOrders);

    }

    @Override
    public void stop() {
        // Stänger bara Hibernate om databasen faktiskt användes.
        if (databaseOnline) {
            HibernateUtil.shutDown();
        }
    }

    // ===================== Header =====================

    private VBox createHeader() {

        Label title = new Label("WIGELL AUTOCORE");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("A Wigell Group Company");
        subtitle.getStyleClass().add("app-subtitle");

        // Företagsnamnet ligger till vänster i headern.
        VBox nameBlock = new VBox(2, title, subtitle);

        // Visar namnet på vald kategori, t.ex. "CUSTOMERS".
        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("category-label");

        // Här hamnar knapparna för vald kategori, centrerat i headern.
        categoryArea = new HBox(10);
        categoryArea.getStyleClass().add("category-area");
        categoryArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(categoryArea, Priority.ALWAYS);

        HBox topRow = new HBox(30, nameBlock, categoryLabel, categoryArea);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(topRow);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(18, 28, 18, 28));

        return header;
    }

    // ===================== Sidomeny =====================

    private ScrollPane createMenu() {

        VBox menuBox = new VBox(4);
        menuBox.getStyleClass().add("app-sidebar");

        menuBox.setPadding(new Insets(18, 14, 18, 14));
        menuBox.setPrefWidth(220);

        Button showCustomers =
                createMenuButton("show-customers", "Show customers");

        Button createCustomer =
                createMenuButton("create-customer", "Create customer");

        Button showVehicles =
                createMenuButton("show-vehicles", "Show vehicles");

        Button createVehicle =
                createMenuButton("create-vehicle", "Create vehicle");

        Button showBookings =
                createMenuButton("show-bookings", "Show bookings");

        Button createBooking =
                createMenuButton("create-booking", "Create booking");

        Button showServices =
                createMenuButton("show-services", "Show services");

        Button showMechanics =
                createMenuButton("show-mechanics", "Show mechanics");

        Button showWorkOrders =
                createMenuButton("show-work-orders", "Show work orders");

        Button createWorkOrder =
                createMenuButton("create-work-order", "Create work order");

        Button startWorkOrder =
                createMenuButton("start-work-order", "Start work order");

        Button completeWorkOrder =
                createMenuButton("complete-work-order", "Complete work order");

        Button showInvoices =
                createMenuButton("show-invoices", "Show invoices");

        Button createInvoice =
                createMenuButton("create-invoice", "Create invoice");

        Button showPayments =
                createMenuButton("show-payments", "Show payments");

        Button processPayment =
                createMenuButton("process-payment", "Process payment");

        // Exit ligger kvar i sidomenyn (röd), inte bland kategoriknapparna.
        Button exit = new Button("Exit");
        exit.getStyleClass().add("exit-item");
        exit.setMaxWidth(Double.MAX_VALUE);
        // Exit ska inte få fokus automatiskt vid start (då kunde Enter stänga appen).
        exit.setFocusTraversable(false);

        // Tunn linje mellan kategorierna och Exit.
        Region divider = new Region();
        divider.getStyleClass().add("nav-divider");
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(divider, new Insets(10, 4, 10, 4));

        // DASHBOARD är en egen rubrik utan knappar: klick visar dashboarden.
        dashboardItem = createNavCategory("DASHBOARD");
        dashboardItem.setOnMouseClicked(mouseEvent -> showDashboard());

        // Tom yta som trycker ned Exit till botten av menyn.
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        menuBox.getChildren().addAll(
                dashboardItem,
                createNavSection("CUSTOMERS", showCustomers, createCustomer),
                createNavSection("VEHICLES", showVehicles, createVehicle),
                createNavSection("BOOKINGS", showBookings, createBooking),
                createNavSection("WORKORDERS", showWorkOrders, createWorkOrder, startWorkOrder, completeWorkOrder),
                createNavSection("SERVICE", showServices, showMechanics),
                createNavSection("INVOICE", showInvoices, createInvoice, showPayments, processPayment),
                spacer,
                divider
        );

        // Visar en liten notis om appen kör utan databas.
        if (!databaseOnline) {
            Label offlineLabel = new Label("Database offline · showing sample data");
            offlineLabel.getStyleClass().add("db-status");
            offlineLabel.setWrapText(true);
            menuBox.getChildren().add(offlineLabel);
        }

        menuBox.getChildren().add(exit);


        /*
         * Actions för knapparna. openView(...) markerar knappen
         * och visar vyn som skapas av lambda-uttrycket.
         */

        openView(showCustomers, () -> new CustomerView());
        openView(createCustomer, () -> new CreateCustomerView());
        openView(showVehicles, () -> ShowVehicleView.build());
        openView(createVehicle, () -> CreateVehicleView.build());
        openView(showBookings, () -> new ShowBookingsView().getView());
        openView(createBooking, () -> new CreateBookingView().getView());
        openView(showServices, () -> new ServiceView());
        openView(showMechanics, () -> new MechanicView());
        openView(showWorkOrders, () -> new ShowWorkOrdersView().getView());
        openView(createWorkOrder, () -> new CreateWorkOrderView());
        openView(startWorkOrder, () -> new StartWorkOrderView());
        openView(completeWorkOrder, () -> CompleteWorkOrderView.build());
        openView(showInvoices, () -> ShowInvoiceView.build());
        openView(createInvoice, () -> CreateInvoiceView.build());
        openView(showPayments, () -> new ShowPaymentsView().getView());
        openView(processPayment, () -> new ProcessPaymentView().getView());

        exit.setOnAction(event ->
                Platform.exit());


        ScrollPane scrollPane = new ScrollPane(menuBox);
        scrollPane.getStyleClass().add("app-sidebar-scroll");

        scrollPane.setFitToWidth(true);
        // Gör att menyn fyller hela höjden så Exit hamnar längst ned.
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scrollPane;
    }

    // Skapar en knapp och sparar den under sin sidnyckel,
    // så att Navigator.goTo(pageKey) kan hitta den.
    private Button createMenuButton(String pageKey, String text) {

        Button button = new Button(text);
        button.setMinWidth(Region.USE_PREF_SIZE);
        navButtonsByKey.put(pageKey, button);

        return button;
    }

    // Skapar en klickbar kategorirubrik i sidomenyn.
    private Label createNavCategory(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("nav-category");
        label.setCursor(Cursor.HAND);
        // Hela raden ska vara klickbar, inte bara texten.
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    // En kategori i sidomenyn. Knapparna syns inte i menyn –
    // de flyttas upp till headern först när man klickar på kategorin.
    private Label createNavSection(String sectionTitle, Button... buttons) {
        Label label = createNavCategory(sectionTitle);

        // Samma kod används vid klick på rubriken och när Navigator byter sida
        Runnable showCategory = () -> {
            categoryArea.getChildren().setAll(buttons);
            categoryLabel.setText(sectionTitle);
            setActiveCategory(label);
        };
        label.setOnMouseClicked(mouseEvent ->{
                showCategory.run();

                // Öppnar kategorins första vy (t.ex. "Show vehicles") direkt,
                // så att innehållet alltid hör ihop med vald kategori.
                buttons[0].fire();

        });


        for (Button button : buttons) {
            showCategoryOfButton.put(button, showCategory);
        }

        return label;
    }

    // Kopplar en knapp till en vy. Supplier gör att vyn skapas först
    // vid klick, så att den alltid visar aktuell data.
    private void openView(Button button, Supplier<Node> viewFactory) {
        button.setOnAction(event -> {
            setActiveButton(button);
            try {
                showContent(viewFactory.get());
            } catch (RuntimeException exception) {
                // T.ex. om en vy försöker nå databasen när den är offline.
                exception.printStackTrace();
                Label error = new Label("This page could not be opened. " +
                        "Check the database connection and try again.");
                error.getStyleClass().add("empty-text");
                showContent(error);
            }
        });
    }

    /**
     * Anropas via Navigator.goTo(...) från en vy. Först visas rätt kategori
     * i headern (knapparna + markering i sidomenyn), sedan "klickar" vi på
     * knappen så att vyn visas och knappen markeras. Okänd nyckel ignoreras.
     */
    private void navigateTo(String pageKey) {
        if ("dashboard".equals(pageKey)) {
            showDashboard();
            return;
        }
        Button button = navButtonsByKey.get(pageKey);
        if (button == null) {
            return;
        }
        Runnable showCategory = showCategoryOfButton.get(button);
        if (showCategory != null) {
            showCategory.run();
        }
        button.fire();
    }

    /**
     * Lägger innehållsytan i en ScrollPane. Två saker är viktiga:
     * 1) fitToWidth – innehållet blir lika brett som fönstret (ingen sidscroll).
     * 2) minHeight = synlig höjd – korta vyer fyller ändå hela ytan (så att
     *    t.ex. en tabell med vgrow kan växa), men höga vyer blir högre än
     *    fönstret och då går det att scrolla.
     */
    private ScrollPane createContentScroll(StackPane content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("content-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) ->
                content.setMinHeight(newBounds.getHeight()));
        return scroll;
    }

    // Lägger en vy i innehållsytan. En ny sida börjar alltid högst upp.
    private void showContent(Node view) {
        contentPane.getChildren().setAll(view);
        contentScroll.setVvalue(0);
        // När headerns knappar byts ut hamnar fokus annars automatiskt på
        // första knappen/fältet, som då ser markerat ut. Innehållsytan tar
        // fokus i stället, så att bara den aktiva knappen syns som vald.
        contentPane.requestFocus();
    }

    private void showDashboard() {
        // Tömmer headern eftersom dashboarden inte har några egna knappar.
        categoryArea.getChildren().clear();
        categoryLabel.setText("");
        setActiveCategory(dashboardItem);
        showContent(new DashboardView());
    }

    private void setActiveCategory(Labeled item) {
        if (activeCategory != null) {
            activeCategory.getStyleClass().remove("nav-item-active");
        }
        // Ny kategori = ingen knapp är vald ännu.
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-item-active");
            activeButton = null;
        }
        item.getStyleClass().add("nav-item-active");
        activeCategory = item;
    }

    private void setActiveButton(Labeled item) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-item-active");
        }
        item.getStyleClass().add("nav-item-active");
        activeButton = item;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
