package com.wac.autocore.ui;

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
import com.wac.autocore.ui.views.ShowWorkOrdersView;
import com.wac.autocore.ui.views.CreateBookingView;


public class AutoCoreApp extends Application {

    private StackPane contentPane;

    @Override
    public void start(Stage primaryStage) {

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
                showPage("Create work order"));

        startWorkOrder.setOnAction(event ->
                showPage("Start work order"));

        completeWorkOrder.setOnAction(event ->
                contentPane.getChildren().setAll(CompleteWorkOrderView.build()));

        showInvoices.setOnAction(event ->
                contentPane.getChildren().setAll(ShowInvoiceView.build()));

        createInvoice.setOnAction(event ->
                contentPane.getChildren().setAll(CreateInvoiceView.build()));

        showPayments.setOnAction(event ->
                showPage("Payments"));

        processPayment.setOnAction(event ->
                showPage("Process payment"));

        exit.setOnAction(event ->
                System.exit(0));


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