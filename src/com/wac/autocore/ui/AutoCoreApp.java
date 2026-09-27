package com.wac.autocore.ui;


import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import javafx.application.Platform;
import com.wac.autocore.data.HibernateUtil;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.wac.autocore.ui.views.ShowBookingsView;
import com.wac.autocore.ui.views.ShowPaymentsView;

import com.wac.autocore.ui.views.ShowWorkOrdersView;
import com.wac.autocore.ui.views.CreateBookingView;
import com.wac.autocore.ui.views.ProcessPaymentView;
import com.wac.autocore.ui.views.MechanicScheduleView;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepository;
import javafx.scene.control.Alert;

import java.util.List;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.layout.HBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.InvoiceRepository;
import com.wac.autocore.repository.PaymentRepository;


public class AutoCoreApp extends Application {

    private StackPane contentPane;

    private final LanguageManager language =
            LanguageManager.getInstance();

    @Override
    public void start(Stage primaryStage) {
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

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database error");
            alert.setHeaderText("Could not load data from the database.");
            alert.setContentText(
                    "Check the database connection and restart the application."
            );
            alert.showAndWait();

            javafx.application.Platform.exit();
            return;
        }

        BorderPane root = new BorderPane();

        VBox header = createHeader();
        ScrollPane menu = createMenu();

        // Område där våra olika sidor ska visas
        contentPane = new StackPane();
        contentPane.setPadding(new Insets(30));

        root.setTop(header);
        root.setLeft(menu);
        root.setCenter(contentPane);

        showWelcomePage();

        Scene scene = new Scene(root, 1100, 700);

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
        HibernateUtil.shutDown();
    }

    private VBox createHeader() {

        Label title = new Label("WIGELL AUTOCORE");
        title.setStyle(
                "-fx-font-size: 26px;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitle = new Label();
        subtitle.textProperty().bind(language.text("header.subtitle"));

        MenuButton languageMenu = new MenuButton();
        languageMenu.textProperty().bind(language.text("language.current"));
        languageMenu.accessibleTextProperty().bind(
                language.text("language.label")
        );

        RadioMenuItem swedish = new RadioMenuItem("Svenska");
        RadioMenuItem english = new RadioMenuItem("English");

        ToggleGroup languageGroup = new ToggleGroup();
        swedish.setToggleGroup(languageGroup);
        english.setToggleGroup(languageGroup);

        // Markeringen följer språket som visas.
        Runnable updateSelection = () -> {
            boolean isSwedish =
                    "Svenska".equals(languageMenu.getText());

            swedish.setSelected(isSwedish);
            english.setSelected(!isSwedish);
        };

        languageMenu.textProperty().addListener(
                (observable, oldText, newText) -> updateSelection.run()
        );

        swedish.setOnAction(event -> {
            language.setLanguage("sv");
            updateSelection.run();
        });

        english.setOnAction(event -> {
            language.setLanguage("en");
            updateSelection.run();
        });

        languageMenu.getItems().addAll(swedish, english);
        updateSelection.run();

        HBox languageRow = new HBox(languageMenu);
        languageRow.setAlignment(Pos.CENTER_RIGHT);

        VBox header = new VBox(5, languageRow, title, subtitle);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(20));

        return header;
    }

    private ScrollPane createMenu() {

        VBox menuBox = new VBox(5);

        menuBox.setPadding(new Insets(15));
        menuBox.setPrefWidth(220);

        Button showCustomers =
                createMenuButton("Show customers");

        Button createCustomer =
                createMenuButton("Create customer");

        Button showVehicles =
                createMenuButton("Show vehicles");

        Button createVehicle =
                createMenuButton("Create vehicle");

        Button showBookings =
                createMenuButton("Show bookings");

        Button createBooking =
                createMenuButton("Create booking");

        Button showServices =
                createMenuButton("Show services");

        Button showMechanics =
                createMenuButton("Show mechanics");

        Button showWorkOrders =
                createMenuButton("Show work orders");

        Button createWorkOrder =
                createMenuButton("Create work order");

        Button startWorkOrder =
                createMenuButton("Start work order");

        Button completeWorkOrder =
                createMenuButton("Complete work order");

        Button showInvoices =
                createMenuButton("Show invoices");

        Button createInvoice =
                createMenuButton("Create invoice");

        Button showPayments =
                createMenuButton("Show payments");

        Button processPayment =
                createMenuButton("Process payment");

        Button scheduleButton =
                createMenuButton("Mechanic schedule");

        Button exit =
                createMenuButton("Exit");

        showCustomers.textProperty().bind(language.text("menu.customers"));
        createCustomer.textProperty().bind(language.text("menu.createCustomer"));
        showVehicles.textProperty().bind(language.text("menu.vehicles"));
        createVehicle.textProperty().bind(language.text("menu.createVehicle"));
        showBookings.textProperty().bind(language.text("menu.bookings"));
        createBooking.textProperty().bind(language.text("menu.createBooking"));
        showServices.textProperty().bind(language.text("menu.services"));
        showMechanics.textProperty().bind(language.text("menu.mechanics"));
        showWorkOrders.textProperty().bind(language.text("menu.workOrders"));
        createWorkOrder.textProperty().bind(language.text("menu.createWorkOrder"));
        startWorkOrder.textProperty().bind(language.text("menu.startWorkOrder"));
        completeWorkOrder.textProperty().bind(language.text("menu.completeWorkOrder"));
        showInvoices.textProperty().bind(language.text("menu.invoices"));
        createInvoice.textProperty().bind(language.text("menu.createInvoice"));
        showPayments.textProperty().bind(language.text("menu.payments"));
        processPayment.textProperty().bind(language.text("menu.processPayment"));
        exit.textProperty().bind(language.text("menu.exit"));

        menuBox.getChildren().addAll(
                showCustomers,
                createCustomer,
                showVehicles,
                createVehicle,
                showBookings,
                createBooking,
                showServices,
                showMechanics,
                showWorkOrders,
                createWorkOrder,
                startWorkOrder,
                completeWorkOrder,
                showInvoices,
                createInvoice,
                showPayments,
                processPayment,
                scheduleButton,
                exit
        );


        /*
         * Tillfälliga actions.
         *
         * Just nu visar vi bara vilken sida
         * användaren har valt.
         */

        showCustomers.setOnAction(event ->
                contentPane.getChildren().setAll(new CustomerView()));

        createCustomer.setOnAction(event ->
                contentPane.getChildren().setAll(new CreateCustomerView()));

        showVehicles.setOnAction(event ->
                contentPane.getChildren().setAll(ShowVehicleView.build()));

        createVehicle.setOnAction(event ->
                contentPane.getChildren().setAll(CreateVehicleView.build()));

        showBookings.setOnAction(event -> {

            ShowBookingsView bookingsView =
                    new ShowBookingsView();

            contentPane.getChildren().setAll(
                    bookingsView.getView()
            );
        });


        createBooking.setOnAction(event -> {

            CreateBookingView createBookingView =
                    new CreateBookingView();

            contentPane.getChildren().setAll(
                    createBookingView.getView()
            );
        });

        showServices.setOnAction(event ->
                contentPane.getChildren().setAll(new ServiceView()));

        showMechanics.setOnAction(event ->
                contentPane.getChildren().setAll(new MechanicView()));

        showWorkOrders.setOnAction(event -> {

            ShowWorkOrdersView workOrdersView =
                    new ShowWorkOrdersView();

            contentPane.getChildren().setAll(
                    workOrdersView.getView()
            );
        });

        createWorkOrder.setOnAction(event ->
                contentPane.getChildren().setAll(new CreateWorkOrderView()));

        startWorkOrder.setOnAction(event ->
                contentPane.getChildren().setAll(new StartWorkOrderView()));

        completeWorkOrder.setOnAction(event ->
                contentPane.getChildren().setAll(CompleteWorkOrderView.build()));

        showInvoices.setOnAction(event ->
                contentPane.getChildren().setAll(ShowInvoiceView.build()));

        createInvoice.setOnAction(event ->
                contentPane.getChildren().setAll(CreateInvoiceView.build()));

        showPayments.setOnAction(event -> {

            ShowPaymentsView paymentsView =
                    new ShowPaymentsView();

            contentPane.getChildren().setAll(
                    paymentsView.getView()
            );
        });

        processPayment.setOnAction(event -> {

            ProcessPaymentView paymentView =
                    new ProcessPaymentView();

            contentPane.getChildren().setAll(
                    paymentView.getView()
            );
        });

        scheduleButton.setOnAction(actionEvent -> {
            MechanicScheduleView scheduleView = new MechanicScheduleView();

            contentPane.getChildren().setAll(
                    scheduleView.getView()
            );
        });

        exit.setOnAction(event ->
                Platform.exit());


        ScrollPane scrollPane = new ScrollPane(menuBox);

        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private Button createMenuButton(String text) {

        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);

        return button;
    }

    private void showWelcomePage() {

        Label welcome = new Label(
                "Welcome to Wigell AutoCore"
        );

        welcome.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        contentPane.getChildren().setAll(welcome);
    }

    private void showPage(String pageName) {

        Label pageTitle = new Label(pageName);

        pageTitle.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        contentPane.getChildren().setAll(pageTitle);
    }

    public static void main(String[] args) {
        launch(args);
    }
}