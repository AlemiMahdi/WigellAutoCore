package com.wac.autocore.ui;

import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.data.Database;
import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import com.wac.autocore.ui.views.CreateBookingView;
import com.wac.autocore.ui.views.ProcessPaymentView;
import com.wac.autocore.ui.views.ShowBookingsView;
import com.wac.autocore.ui.views.ShowPaymentsView;
import com.wac.autocore.ui.views.ShowWorkOrdersView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;



import java.util.HashMap;

import com.wac.autocore.ui.views.MechanicScheduleView;

import java.util.List;
import java.util.Map;


/**
 * Huvudfönstret. Variant A: sidomenyn visar alla sektioner och alla knappar
 * direkt (som i designen i PDF:en). Appen startar alltid på Dashboard.
 */

public class AutoCoreApp extends Application {

    private StackPane contentPane;

    // ScrollPane runt innehållsytan, så att höga vyer går att scrolla
    private ScrollPane contentScroll;

    // Det menyval som just nu är markerat, så vi kan avmarkera det vid byte
    private Button activeNavItem;

    // Alla menyval, med sidnyckel (t.ex. "create-customer") som nyckel.
    // Används av Navigator så att en knapp i en vy kan "klicka" på menyn.
    private final Map<String, Button> navItemsByKey = new HashMap<String, Button>();

    // Sant om data inte gick att läsa från MySQL vid start
    private boolean offlineMode = false;

    private final LanguageManager language = LanguageManager.getInstance();

    @Override
    public void start(Stage primaryStage) {
        loadDataFromDatabase();

        // Område där våra olika sidor ska visas
        contentPane = new StackPane();
        contentPane.getStyleClass().add("app-content");
        contentPane.setPadding(new Insets(30));
        contentPane.setAlignment(Pos.TOP_LEFT);

        // Höga vyer (t.ex. dashboardens kanban) ska gå att scrolla till.
        // Därför ligger hela innehållsytan i en ScrollPane.
        contentScroll = createContentScroll(contentPane);

        BorderPane centerArea = new BorderPane(contentScroll);
        if (offlineMode) {
            centerArea.setTop(createOfflineBanner());
        }

        BorderPane root = new BorderPane();
        root.setTop(createHeader());
        root.setLeft(createMenu());
        root.setCenter(centerArea);

        // Knappar inne i vyerna (t.ex. "+ New customer") byter sida via
        // Navigator. Vi gör samma sak som när man klickar i menyn.
        Navigator.setHandler(this::navigateTo);

        // Dashboard är startsidan
        showDashboard();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(AutoCoreApp.class.getResource("styles.css").toExternalForm());

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    /**
     * Läser in sparad data från MySQL till Database-listorna.
     * Om databasen inte går att nå kraschar vi INTE – appen visas ändå
     * med exempeldatan, och en gul banner berättar att inget sparas.
     */
    private void loadDataFromDatabase() {
        try {
            loadCustomers();
            loadVehicles();
            loadServiceItems();
            loadMechanics();
            loadBookings();
            loadWorkOrders();
            loadInvoices();
            loadPayments();

        } catch (RuntimeException exception) {
            exception.printStackTrace();
            offlineMode = true;
        }
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


    private void loadInvoices() {

        InvoiceRepository repository =
                new InvoiceRepository();

        List<Invoice> savedInvoices =
                repository.findAllInvoices();

        if (savedInvoices.isEmpty()) {

            // Sparar eventuell befintlig data första gången.
            for (Invoice invoice : Database.getInvoices()) {
                repository.save(invoice);
            }

            return;
        }

        // Originalsystemet använder listans storlek + 1 för nästa ID.
        for (int i = 0; i < savedInvoices.size(); i++) {
            if (savedInvoices.get(i).getId() != i + 1) {
                throw new IllegalStateException(
                        "Invoice IDs must be consecutive, starting at 1."
                );
            }
        }

        Database.getInvoices().clear();
        Database.getInvoices().addAll(savedInvoices);
    }

    private void loadPayments() {

        PaymentRepository repository =
                new PaymentRepository();

        List<Payment> savedPayments =
                repository.findAllPayments();

        if (savedPayments.isEmpty()) {

            // Sparar eventuell befintlig data första gången.
            for (Payment payment : Database.getPayments()) {
                repository.save(payment);
            }

            return;
        }

        // Originalsystemet använder listans storlek + 1 för nästa ID.
        for (int i = 0; i < savedPayments.size(); i++) {

            Payment payment = savedPayments.get(i);

            if (payment.getId() != i + 1) {
                throw new IllegalStateException(
                        "Payment IDs must be consecutive, starting at 1."
                );
            }

            // En betalning måste höra till en befintlig faktura.
            boolean invoiceExists =
                    Database.getInvoices().stream()
                            .anyMatch(invoice ->
                                    invoice.getId() == payment.getInvoiceId());

            if (!invoiceExists) {
                throw new IllegalStateException(
                        "Invoice missing for payment " + payment.getId()
                );
            }
        }

        Database.getPayments().clear();
        Database.getPayments().addAll(savedPayments);
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
            if (workOrder.getId() != i + 1) {
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
        HibernateUtil.shutDown();
    }

    // ------------------------------------------------------------
    // Header och offline-banner
    // ------------------------------------------------------------

    private StackPane createHeader() {
        Label title = new Label("WIGELL AUTOCORE");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label();
        subtitle.textProperty().bind(language.text("header.subtitle"));
        subtitle.getStyleClass().add("app-subtitle");

        // Rubrik och underrubrik ligger centrerade mitt i sidhuvudet
        VBox titleBox = new VBox(4, title, subtitle);
        titleBox.setAlignment(Pos.CENTER);

        // Språkväljaren läggs ovanpå, men skjuts ut till höger kant
        MenuButton languageMenu = createLanguageMenu();
        StackPane.setAlignment(languageMenu, Pos.CENTER_RIGHT);

        StackPane header = new StackPane(titleBox, languageMenu);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(18));
        return header;
    }

    /** Knapp med rullgardinsmeny för att byta språk (Svenska/English).*/
    private MenuButton createLanguageMenu() {
        MenuButton languageMenu = new MenuButton();
        languageMenu.textProperty().bind(language.text("language.current"));
        languageMenu.accessibleTextProperty().bind(language.text("language.label"));
        languageMenu.getStyleClass().add("language-menu");

        // Språknamnet översätts inte: man skriver alltid språket på sitt eget språk
        RadioMenuItem swedish = new RadioMenuItem("Svenska");
        RadioMenuItem english = new RadioMenuItem("English");

        // ToggleGroup: bara ett av valen kan vara markerat åt gången
        ToggleGroup languageGroup = new ToggleGroup();
        swedish.setToggleGroup(languageGroup);
        english.setToggleGroup(languageGroup);
        swedish.setSelected(true); // appen startar på svenska

        swedish.setOnAction(actionEvent -> language.setLanguage("sv"));
        english.setOnAction(actionEvent -> language.setLanguage("en"));

        languageMenu.getItems().addAll(swedish, english);
        return languageMenu;
    }

    private HBox createOfflineBanner() {
        Label message = new Label(
                "Database not reachable – showing sample data. Changes will not be saved.");
        HBox banner = new HBox(message);
        banner.getStyleClass().add("offline-banner");
        return banner;
    }

    // ------------------------------------------------------------
    // Sidomenyn
    // ------------------------------------------------------------

    private ScrollPane createMenu() {
        // Tätt radavstånd så att hela menyn får plats i 1280x800 utan scrollbar
        VBox menuBox = new VBox(1);
        menuBox.getStyleClass().add("app-sidebar");
        menuBox.setPadding(new Insets(8, 14, 10, 14));
        menuBox.setPrefWidth(250);

        // Dashboard överst så man alltid kan komma tillbaka till startsidan
        Button dashboard = createNavItem("dashboard", "menu.dashboard", this::showDashboard);
        setActiveNavItem(dashboard);
        menuBox.getChildren().add(dashboard);

        addNavSection(menuBox, "menu.section.customersVehicles",
                createNavItem("show-customers", "menu.customers", () -> showView(new CustomerView())),
                createNavItem("create-customer", "menu.createCustomer", () -> showView(new CreateCustomerView())),
                createNavItem("show-vehicles", "menu.vehicles", () -> showView(ShowVehicleView.build())),
                createNavItem("create-vehicle", "menu.createVehicle", () -> showView(CreateVehicleView.build())));

        addNavSection(menuBox, "menu.section.bookings",
                createNavItem("show-bookings", "menu.bookings", () -> showView(new ShowBookingsView().getView())),
                createNavItem("create-booking", "menu.createBooking", () -> showView(new CreateBookingView().getView())));

        addNavSection(menuBox, "menu.section.workOrders",
                createNavItem("show-work-orders", "menu.workOrders", () -> showView(new ShowWorkOrdersView().getView())),
                createNavItem("create-work-order", "menu.createWorkOrder", () -> showView(new CreateWorkOrderView())),
                createNavItem("start-work-order", "menu.startWorkOrder", () -> showView(new StartWorkOrderView())),
                createNavItem("complete-work-order", "menu.completeWorkOrder", () -> showView(CompleteWorkOrderView.build())));

        addNavSection(menuBox, "menu.section.servicesMechanics",
                createNavItem("show-services", "menu.services", () -> showView(new ServiceView())),
                createNavItem("show-mechanics", "menu.mechanics", () -> showView(new MechanicView())),
                createNavItem("mechanic-schedule", "menu.schedule", () -> showView(new MechanicScheduleView().getView())));

        addNavSection(menuBox, "menu.section.invoicesPayments",
                createNavItem("show-invoices", "menu.invoices", () -> showView(ShowInvoiceView.build())),
                createNavItem("create-invoice", "menu.createInvoice", () -> showView(CreateInvoiceView.build())),
                createNavItem("show-payments", "menu.payments", () -> showView(new ShowPaymentsView().getView())),
                createNavItem("process-payment", "menu.processPayment", () -> showView(new ProcessPaymentView().getView())));

        // Tunn linje och sedan Exit längst ned, i rött
        Region divider = new Region();
        divider.getStyleClass().add("nav-divider");
        VBox.setMargin(divider, new Insets(8, 4, 6, 4));


        Button exit = new Button();
        exit.textProperty().bind(language.text("menu.exit"));
        exit.getStyleClass().addAll("nav-item", "exit-item");
        exit.setMaxWidth(Double.MAX_VALUE);
        exit.setOnAction(event -> Platform.exit());


        menuBox.getChildren().addAll(divider, exit);

        ScrollPane scrollPane = new ScrollPane(menuBox);
        scrollPane.getStyleClass().add("sidebar-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // Scrollbaren visas bara om fönstret är lägre än menyn,
        // och då som en smal diskret list (se .sidebar-scroll i CSS).
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        return scrollPane;
    }

    /**
     * Lägger till en sektionsrubrik följd av sektionens knappar.
     */
    private void addNavSection(VBox menuBox, String titleKey, Button... items) {
        Label label = new Label();
        label.textProperty().bind(language.text(titleKey));
        label.getStyleClass().add("nav-section-label");
        menuBox.getChildren().add(label);
        menuBox.getChildren().addAll(items);
    }

    /**
     * Skapar ett menyval. Runnable = "koden som ska köras vid klick",
     * så varje knapp kan visa sin egen vy utan att vi upprepar
     * markera-aktiv-logiken sexton gånger. pageKey sparas så att
     * Navigator.goTo(pageKey) kan hitta knappen.
     */
    private Button createNavItem(String pageKey, String textKey, Runnable onClick) {
        Button button = new Button();
        button.textProperty().bind(language.text(textKey));
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            setActiveNavItem(button);
            onClick.run();
        });
        navItemsByKey.put(pageKey, button);
        return button;
    }

    /**
     * Anropas via Navigator.goTo(...) från en vy. Vi "klickar" på
     * menyknappen, så att både vyn och den aktiva markeringen byts.
     * Okänd nyckel ignoreras.
     */
    private void navigateTo(String pageKey) {
        Button item = navItemsByKey.get(pageKey);
        if (item != null) {
            item.fire();
        }
    }

    private void setActiveNavItem(Button item) {
        if (activeNavItem != null) {
            activeNavItem.getStyleClass().remove("nav-item-active");
        }
        item.getStyleClass().add("nav-item-active");
        activeNavItem = item;
    }

    // ------------------------------------------------------------
    // Byte av vy i innehållsytan
    // ------------------------------------------------------------

    /**
     * Lägger innehållsytan i en ScrollPane. Två saker är viktiga:
     * 1) fitToWidth – innehållet blir lika brett som fönstret (ingen sidscroll).
     * 2) minHeight = synlig höjd – korta vyer fyller ändå hela ytan (så att
     * t.ex. en tabell med vgrow kan växa), men höga vyer blir högre än
     * fönstret och då går det att scrolla.
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

    private void showView(Node view) {
        contentPane.getChildren().setAll(view);
        // Ny sida ska alltid börja högst upp
        contentScroll.setVvalue(0);
    }

    private void showDashboard() {
        // Byggs om varje gång så att siffrorna alltid är aktuella.
        // (Scrollningen sköts av innehållsytans ScrollPane.)
        showView(new DashboardView());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
