package com.wac.autocore.ui;

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


public class AutoCoreApp extends Application {

    private StackPane contentPane;

    @Override
    public void start(Stage primaryStage) {
        try {
            loadCustomers();
            loadVehicles();
            loadServiceItems();
            loadMechanics();

        } catch (RuntimeException exception) {
            exception.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database error");
            alert.setHeaderText("Customers or vehicles could not be loaded.");
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

        Label subtitle = new Label("A Wigell Group Company");

        VBox header = new VBox(5, title, subtitle);

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

        Button exit =
                createMenuButton("Exit");


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