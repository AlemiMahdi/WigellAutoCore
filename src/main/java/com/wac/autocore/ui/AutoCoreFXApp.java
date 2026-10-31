package com.wac.autocore.ui;

import java.net.URL;

import com.wac.autocore.service.*;
import com.wac.autocore.ui.language.LanguageManager;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import com.wac.autocore.security.AuthenticationService;
import com.wac.autocore.security.Role;
import com.wac.autocore.security.actions.SecuredBookingActions;
import com.wac.autocore.security.actions.SecuredCustomerActions;
import com.wac.autocore.security.actions.SecuredServiceItemActions;
import com.wac.autocore.security.actions.SecuredServicePackageActions;
import com.wac.autocore.security.actions.SecuredVehicleActions;
import com.wac.autocore.security.actions.SecuredWorkOrderActions;
import com.wac.autocore.security.actions.SecuredInvoiceActions;
import com.wac.autocore.security.actions.SecuredMechanicActions;
import com.wac.autocore.security.actions.SecuredPaymentActions;
import com.wac.autocore.ui.views.*;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.wac.autocore.SpringBootMain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;


/**
 * Huvudfönstret. Variant A: sidomenyn visar alla sektioner och alla knappar
 * direkt (som i designen i PDF:en). Appen startar alltid på Dashboard.
 */

public class AutoCoreFXApp extends Application {

    private static AutoCoreFXApp instance;

    private InvoiceService invoiceService;
    private MechanicService mechanicService;
    private VehicleService vehicleService;
    private ServicePackageService servicePackageService;
    private WorkOrderService workOrderService;
    private CustomerService customerService;
    private BookingService bookingService;
    private PaymentService paymentService;
    private ServiceItemService serviceItemService;
    private SecuredCustomerActions securedCustomerActions;
    private AuthenticationService authenticationService;
    private BorderPane appRoot;
    private SecuredVehicleActions securedVehicleActions;
    private SecuredBookingActions securedBookingActions;
    private SecuredWorkOrderActions securedWorkOrderActions;
    private SecuredServiceItemActions securedServiceItemActions;
    private SecuredInvoiceActions securedInvoiceActions;
    private SecuredPaymentActions securedPaymentActions;
    private SecuredMechanicActions securedMechanicActions;
    private SecuredServicePackageActions securedServicePackageActions;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AutoCoreFXApp.class);

    private StackPane contentPane;

    // ScrollPane runt innehållsytan, så att höga vyer går att scrolla
    private ScrollPane contentScroll;

    // Det menyval som just nu är markerat, så vi kan avmarkera det vid byte
    private Button activeNavItem;

    // Alla menyval, med sidnyckel (t.ex. "create-customer") som nyckel.
    // Används av Navigator så att en knapp i en vy kan "klicka" på menyn.
    private final Map<String, Button> navItemsByKey = new HashMap<>();

    // Sant om data inte gick att läsa från MySQL vid start
    private boolean offlineMode = false;

    private final LanguageManager language = LanguageManager.getInstance();

    private ConfigurableApplicationContext springContext;
    //Injecera repositoryes med @Autowired?



    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.springContext = new SpringApplicationBuilder()
                .sources(SpringBootMain.class)
                .headless(false)
                .run(args);

        // Eftersom JavaFX skapar instansen av AutocoreApp manuellt, måste vi be
        // Springs context att köra "autowire" på den här instansen så fälten blir tillgängliga
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }
    
    @Override
    public void start(Stage primaryStage) {

        getServicesFromSpring();

        authenticationService = springContext.getBean(AuthenticationService.class);

        // Börja med ett fönster som endast visar inloggningen.
        appRoot = new BorderPane();

        showLogin();

        Scene scene = new Scene(appRoot, 1280, 800);

        URL stylesheet =
                AutoCoreFXApp.class.getResource("/style/styles.css");

        if (stylesheet == null) {
            throw new IllegalStateException(
                    "Kunde inte hitta styles.css"
            );
        }

        scene.getStylesheets().add(stylesheet.toExternalForm());

        primaryStage.setTitle("Wigell AutoCore");

        primaryStage.getIcons().addAll(
                loadImage("/images/logo-gron-16.png"),
                loadImage("/images/logo-gron-32.png"),
                loadImage("/images/logo-gron-48.png"),
                loadImage("/images/logo-gron-256.png")
        );

        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void showLogin() {
        appRoot.setTop(null);
        appRoot.setLeft(null);

        appRoot.setCenter(new LoginView(
            authenticationService, 
            () -> showMainApplication(appRoot)));
    }

    private void logout() {

        //Tar bort användarens autentisering
        authenticationService.logout();;
        
        //Rensa den gamla användarens navigering
        navItemsByKey.clear();
        activeNavItem = null;

        //Visa inloggningssidan igen
        showLogin();
    }

    private void showMainApplication(BorderPane root) {
        loadDataFromDatabase();
        contentPane = new StackPane();
        contentPane.getStyleClass().add("app-content");
        contentPane.setPadding(new Insets(30));
        contentPane.setAlignment(Pos.TOP_LEFT);
        
        contentScroll = createContentScroll(contentPane);
        BorderPane centerArea = new BorderPane(contentScroll);

        if (offlineMode) {
            centerArea.setTop(createOfflineBanner());
        }

        root.setTop(createHeader());
        root.setLeft(createMenu());
        root.setCenter(centerArea);

        Navigator.setHandler(this::navigateTo);
        showDashboard();


    }


    //hjälpmetoden
    private void getServicesFromSpring() {
        workOrderService = springContext.getBean(WorkOrderService.class);
        bookingService = springContext.getBean(BookingService.class);
        customerService = springContext.getBean(CustomerService.class);
        paymentService = springContext.getBean(PaymentService.class);
        invoiceService = springContext.getBean(InvoiceService.class);
        vehicleService = springContext.getBean(VehicleService.class);
        servicePackageService = springContext.getBean(ServicePackageService.class);
        serviceItemService = springContext.getBean(ServiceItemService.class);
        mechanicService = springContext.getBean(MechanicService.class);
        securedVehicleActions = springContext.getBean(SecuredVehicleActions.class);
        securedBookingActions = springContext.getBean(SecuredBookingActions.class);
        securedCustomerActions = springContext.getBean(SecuredCustomerActions.class);
        securedWorkOrderActions = springContext.getBean(SecuredWorkOrderActions.class);
        securedServiceItemActions = springContext.getBean(SecuredServiceItemActions.class);
        securedInvoiceActions = springContext.getBean(SecuredInvoiceActions.class);
        securedPaymentActions = springContext.getBean(SecuredPaymentActions.class);
        securedMechanicActions = springContext.getBean(SecuredMechanicActions.class);
        securedServicePackageActions = springContext.getBean(SecuredServicePackageActions.class);
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
            loadServicePackages();
            loadMechanics();
            loadBookings();
            loadWorkOrders();
            loadInvoices();
            loadPayments();

        } catch (RuntimeException exception) {
            LOGGER.error("Could not load data from database", exception);
            offlineMode = true;
        }
    }

    private void loadCustomers() {
        CustomerRepository repository = springContext.getBean(CustomerRepository.class);
        List<Customer> savedCustomers = repository.findAll();

        if (savedCustomers.isEmpty()) {

            Map<Integer, Integer> customerIdMapping = new HashMap<>();

            // Spara exempelkunder och registrera deras nya ID:n
            for (Customer customer : Database.getCustomers()) {

                //Spara ursprungligt ID för att kunna uppdatera fordonen
                int oldId = customer.getId();

                //Låt MySQL generera ett nytt ID
                customer.setId(0);
                repository.save(customer);

                //Hämta det ID som MySQL genererade
                int newId = customer.getId();
                customerIdMapping.put(oldId, newId);
            }

            // Uppdatera exempelfordonens kopplingar till kunderna
            for (Vehicle vehicle : Database.getVehicles()) {

                Integer newCustomerId =
                        customerIdMapping.get(vehicle.getCustomerId());

                if (newCustomerId != null) {
                    vehicle.setCustomerId(newCustomerId);
                }
            }

            return;
        }

        // Ersätter kunderna i minnet med de sparade kunderna.
        Database.getCustomers().clear();
        Database.getCustomers().addAll(savedCustomers);
    }
    // Läser in fordon efter att kunderna har laddats.

    private void loadVehicles() {
        VehicleRepository repository = springContext.getBean(VehicleRepository.class);
        List<Vehicle> savedVehicles = repository.findAll();

        boolean firstRun = savedVehicles.isEmpty();

        List<Vehicle> vehicles = firstRun
                ? Database.getVehicles()
                : savedVehicles;

        for (Vehicle vehicle : vehicles) {
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

            Map<Integer, Integer> vehicleIdMapping = new HashMap<>();

            // Spara exempelfordon och registrera deras nya ID:n.
            for (Vehicle vehicle : vehicles) {

                int oldId = vehicle.getId();

                // Låt MySQL generera ett nytt ID.
                vehicle.setId(0);
                repository.save(vehicle);

                int newId = vehicle.getId();
                vehicleIdMapping.put(oldId, newId);
            }

            // Uppdatera exempelbokningarnas fordonskopplingar.
            for (Booking booking : Database.getBookings()) {

                Integer newVehicleId =
                        vehicleIdMapping.get(booking.getVehicleId());

                if (newVehicleId != null) {
                    booking.setVehicleId(newVehicleId);
                }
            }
        } else {
                    // Återställer sparade fordon i programmets minne.
                    Database.getVehicles().clear();
                    Database.getVehicles().addAll(savedVehicles);
                }
            }

    private void loadServiceItems() {
        ServiceItemRepository repository = springContext.getBean(ServiceItemRepository.class);
        List<ServiceItem> savedServiceItems = repository.findAll();

        if (savedServiceItems.isEmpty()) {
            // Första starten: sparar originalets exempeldata.
            for (ServiceItem serviceItem : Database.getServiceItems()) {
                repository.save(serviceItem);
            }
            return;
        }
        // Ersätter minnesdatan med datan från databasen.
        Database.getServiceItems().clear();
        Database.getServiceItems().addAll(savedServiceItems);
    }

    private void loadServicePackages() {
        ServicePackageRepository repository = springContext.getBean(ServicePackageRepository.class);
        List<ServicePackage> savedPackages = repository.findAll();
        Database.getServicePackages().clear();
        Database.getServicePackages().addAll(savedPackages);
    }
    private void loadMechanics() {

        List<Mechanic> savedMechanics =
                mechanicService.findAll();
        if (savedMechanics.isEmpty()) {

            // Första starten: sparar originalets exempeldata.
            for (Mechanic mechanic : Database.getMechanics()) {
                mechanicService.save(mechanic);
            }

            return;
        }

        // Ersätter minnesdatan med datan från databasen.
        Database.getMechanics().clear();
        Database.getMechanics().addAll(savedMechanics);

    }


    private void loadInvoices() {

        List<Invoice> savedInvoices = invoiceService.findAll();
        if (savedInvoices.isEmpty()) {
            // Sparar eventuell befintlig data första gången.
            for (Invoice invoice : Database.getInvoices()) {
                invoiceService.save(invoice);
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
        PaymentRepository repository = springContext.getBean(PaymentRepository.class);
        List<Payment> savedPayments = repository.findAll();

        if (savedPayments.isEmpty()) {
            // Sparar eventuell befintlig data första gången.
            for (Payment payment : Database.getPayments()) {
                repository.save(payment);
            }
            return;
        }
        // Originalsystemet använder listans storlek + 1 för nästa ID.
        for ( Payment payment : savedPayments) {
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
        BookingRepository repository = springContext.getBean(BookingRepository.class);
        List<Booking> savedBookings = repository.findAll();
        // Tom tabell betyder att appen startas för första gången.
        boolean firstRun = savedBookings.isEmpty();
        // Första gången kontrolleras originalets exempeldata,
        // annars de bokningar som hämtats från databasen.
        List<Booking> bookings = firstRun ? Database.getBookings() : savedBookings;
        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
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
                booking.setId(0);
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
        WorkOrderRepository repository = springContext.getBean(WorkOrderRepository.class);
        List<WorkOrder> savedWorkOrders = repository.findAll();
        // Originalet har inga exempelarbetsordrar,
        // så en tom tabell betyder att det inte finns något att läsa in.
        if (savedWorkOrders.isEmpty()) {
            return;
        }
        Database.getWorkOrders().clear();
        Database.getWorkOrders().addAll(savedWorkOrders);
    }

    // ------------------------------------------------------------
    // Header och offline-banner
    // ------------------------------------------------------------

    private StackPane createHeader() {
        // Kundens logga visas centrerad i sidhuvudet
        ImageView logo = new ImageView(loadImage("/images/logga-utan-underrubrik.png"));
        logo.setPreserveRatio(true);
        logo.setFitHeight(60);
        logo.setSmooth(true);
        logo.setAccessibleText("Wigell AutoCore");

        // Underrubrik som text, så att den byter språk
        Label subtitle = new Label();
        subtitle.textProperty().bind(language.text("header.subtitle"));
        subtitle.getStyleClass().add("app-subtitle");

        // Loggan med underrubriken under. Paketet centreras i headern,
        // men inuti ligger allt vänsterställt.
        VBox brand = new VBox(4, logo, subtitle);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE); // brand blir inte bredare än loggan

        // Lägg underrubriken tätt under "Wigell".
        // Måtten är uppmätta i loggbilden (60 px hög): texten börjar 69 px in
        // och det är tom yta under den. Ändras loggan måste måtten ändras.
        VBox.setMargin(subtitle, new Insets(-19, 0, 0, 69));

        // Språkväljaren läggs ovanpå, men skjuts ut till höger kant
        MenuButton languageMenu = createLanguageMenu();
        StackPane.setAlignment(languageMenu, Pos.CENTER_RIGHT);

        StackPane header = new StackPane(brand, languageMenu);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(18));
        return header;
    }

    // Laddar en bild från resources i originalstorlek. Sökvägen ska börja med "/".
    private Image loadImage(String path) {
        return loadImage(path, 0);
    }

    // Laddar en bild och skalar den till en viss höjd vid inläsning (skarpare).
    private Image loadImage(String path, double height) {
        URL url = AutoCoreFXApp.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Kunde inte hitta bilden " + path);
        }
        return new Image(url.toExternalForm(), 0, height, true, true);
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
        Label message = new Label();
        message.textProperty().bind(language.text("app.offline"));
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

        
        boolean canEdit =
                authenticationService.hasRole(Role.ADMIN)
                || authenticationService.hasRole(Role.WORKSHOP);

        List<Button> customerVehicleItems = new ArrayList<>();

        customerVehicleItems.add(
                createNavItem("show-customers", "menu.customers",
                        () -> showView(new CustomerView(securedCustomerActions, canEdit)))
        );

        if (canEdit) {
            customerVehicleItems.add(
                    createNavItem("create-customer", "menu.createCustomer",
                            () -> showView(
                                    new CreateCustomerView(securedCustomerActions)
                            ))
            );
        }

        customerVehicleItems.add(
                createNavItem("show-vehicles", "menu.vehicles",
                        () -> showView(ShowVehicleView.build(securedVehicleActions, canEdit)))
        );

        if (canEdit) {
            customerVehicleItems.add(
                    createNavItem("create-vehicle", "menu.createVehicle",
                            () -> showView(CreateVehicleView.build(securedVehicleActions)))
            );
        }

        addNavSection(
                menuBox,
                "menu.section.customersVehicles",
                customerVehicleItems.toArray(new Button[0])
        );


        
        List<Button> bookingItems = new ArrayList<>();
        // Alla inloggade användare får visa bokningar
        bookingItems.add(
                createNavItem("show-bookings", "menu.bookings",
                        () -> showView(
                                new ShowBookingsView(securedBookingActions, canEdit).getView()
                        ))
        );
        // Endast ADMIN och WORKSHOP får skapa bokningar
        if (canEdit) {
            bookingItems.add(
                    createNavItem("create-booking", "menu.createBooking",
                            () -> showView(
                                    new CreateBookingView(securedBookingActions).getView()
                            ))
            );
        }
        addNavSection(
                menuBox,
                "menu.section.bookings",
                bookingItems.toArray(new Button[0])
        );


        
        List<Button> workOrderItems = new ArrayList<>();

        // Alla inloggade användare får visa arbetsordrar
        workOrderItems.add(
                createNavItem("show-work-orders", "menu.workOrders",
                        () -> showView(
                                new ShowWorkOrdersView(
                                        securedWorkOrderActions,
                                        securedBookingActions,
                                        canEdit
                                ).getView()
                        ))
        );
        // Endast ADMIN och WORKSHOP får hantera arbetsordrar
        if (canEdit) {
            workOrderItems.add(
                    createNavItem("create-work-order", "menu.createWorkOrder",
                            () -> showView(
                                    new CreateWorkOrderView(
                                            securedWorkOrderActions,
                                            securedBookingActions
                                    )
                            ))
            );

            workOrderItems.add(
                    createNavItem("create-draft-work-order", "menu.createDraftWorkOrder",
                            () -> showView(
                                    new CreateDraftWorkOrderView(securedWorkOrderActions).getView()
                            ))
            );

            workOrderItems.add(
                    createNavItem("start-work-order", "menu.startWorkOrder",
                            () -> showView(
                                    new StartWorkOrderView(securedWorkOrderActions)
                            ))
            );

            workOrderItems.add(
                    createNavItem("complete-work-order", "menu.completeWorkOrder",
                            () -> showView(
                                    new CompleteWorkOrderView(securedWorkOrderActions).getView()
                            ))
            );
        }

        addNavSection(
                menuBox,
                "menu.section.workOrders",
                workOrderItems.toArray(new Button[0])
        );


        
        boolean isAdmin =
                authenticationService.hasRole(Role.ADMIN);

        List<Button> serviceMechanicItems = new ArrayList<>();

        // Alla inloggade användare får visa tjänster
        serviceMechanicItems.add(
                createNavItem("show-services", "menu.services",
                        () -> showView(
                                new ServiceView(
                                    securedServiceItemActions,
                                    canEdit,
                                    isAdmin
                                )
                        ))
        );

        // Endast ADMIN får skapa och ändra servicepaket
        if (isAdmin) {

            serviceMechanicItems.add(
                    createNavItem("create-service-package",
                            "menu.createServicePackage",
                            () -> showView(
                                    new CreateServicePackageView(
                                            securedServicePackageActions
                                    ).getView()
                            ))
            );

            serviceMechanicItems.add(
                    createNavItem("manage-service-package",
                            "menu.manageServicePackage",
                            () -> showView(
                                    new ManageServicePackageView(
                                            securedServicePackageActions
                                    ).getView()
                            ))
            );
        }

        // Alla inloggade användare får visa mekaniker
        serviceMechanicItems.add(
                createNavItem("show-mechanics", "menu.mechanics",
                        () -> showView(
                                new MechanicView(securedMechanicActions)
                        ))
        );

        // Alla inloggade användare får visa scheman
        serviceMechanicItems.add(
                createNavItem("mechanic-schedule", "menu.schedule",
                        () -> showView(
                                new MechanicScheduleView(
                                        securedMechanicActions
                                ).getView()
                        ))
        );

        addNavSection(
                menuBox,
                "menu.section.servicesMechanics",
                serviceMechanicItems.toArray(new Button[0])
        );


        
        List<Button> invoicePaymentItems = new ArrayList<>();

        // Alla inloggade användare får visa fakturor
        invoicePaymentItems.add(
                createNavItem("show-invoices", "menu.invoices",
                        () -> showView(
                                ShowInvoiceView.build(securedInvoiceActions, isAdmin)
                        ))
        );

        // Endast ADMIN får skapa fakturor
        if (isAdmin) {
            invoicePaymentItems.add(
                    createNavItem("create-invoice", "menu.createInvoice",
                            () -> showView(
                                    CreateInvoiceView.build(securedInvoiceActions)
                            ))
            );
        }

        // Alla inloggade användare får visa betalningar
        invoicePaymentItems.add(
                createNavItem("show-payments", "menu.payments",
                        () -> showView(
                                new ShowPaymentsView().getView(securedPaymentActions)
                        ))
        );

        // Endast ADMIN får registrera betalningar
        if (isAdmin) {
            invoicePaymentItems.add(
                    createNavItem("process-payment", "menu.processPayment",
                            () -> showView(
                                    new ProcessPaymentView().getView(
                                            securedPaymentActions
                                    )
                            ))
            );
        }

        addNavSection(
                menuBox,
                "menu.section.invoicesPayments",
                invoicePaymentItems.toArray(new Button[0])
        );

        // Tunn linje och sedan Exit längst ned, i rött
        Region divider = new Region();
        divider.getStyleClass().add("nav-divider");
        VBox.setMargin(divider, new Insets(8, 4, 6, 4));


        Button exit = new Button();
        exit.textProperty().bind(language.text("menu.exit"));
        exit.getStyleClass().addAll("nav-item", "exit-item");
        exit.setMaxWidth(Double.MAX_VALUE);
        exit.setOnAction(event -> Platform.exit());

        //Ny utloggnningsknapp
        Button logoutButton = new Button();
        logoutButton.textProperty().bind(language.text("menu.logout"));
        logoutButton.getStyleClass().add("nav-item");
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        logoutButton.setOnAction(event -> logout());



        menuBox.getChildren().addAll(divider, logoutButton, exit);

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
        VBox content = new VBox(1);
        content.getChildren().addAll(items);

        TitledPane section = new TitledPane();
        Label heading = new Label();
        heading.textProperty().bind(language.text(titleKey));
        heading.getStyleClass().add("nav-section-heading");
        heading.setMouseTransparent(true);

        section.setText("");
        section.setGraphic(heading);
        section.setContent(content);
        section.setExpanded(true);
        section.setAnimated(false);
        section.setMaxWidth(Double.MAX_VALUE);
        section.getStyleClass().add("nav-section");

        menuBox.getChildren().add(section);
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
}
