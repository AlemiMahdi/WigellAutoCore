package com.wac.autocore.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import com.wac.autocore.ui.views.ShowBookingsView;
import com.wac.autocore.ui.views.ShowPaymentsView;
import com.wac.autocore.ui.views.ShowWorkOrdersView;
import com.wac.autocore.ui.views.CreateBookingView;
import com.wac.autocore.ui.views.ProcessPaymentView;


public class AutoCoreApp extends Application {

    private StackPane contentPane;
    private HBox categoryArea;
    private Label categoryLabel;
    private Labeled activeButton;

    @Override
    public void start(Stage primaryStage) {

        BorderPane root = new BorderPane();

        VBox header = createHeader();
        ScrollPane menu = createMenu();

        // Område där våra olika sidor ska visas
        contentPane = new StackPane();
        contentPane.getStyleClass().add("app-content");
        contentPane.setPadding(new Insets(30));

        root.setTop(header);
        root.setLeft(menu);
        root.setCenter(contentPane);

        showWelcomePage();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        primaryStage.setTitle("Wigell AutoCore");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createHeader() {

        Label title = new Label("WIGELL AUTOCORE");
        title.getStyleClass().add("app-title");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        Label subtible = new Label("A Wigell Group Company");
        subtible.getStyleClass().add("app-subtitle");

        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("category-label");

        VBox nameBlock = new VBox(5, title, subtible);

        categoryArea = new HBox(10);
        categoryArea.setAlignment(Pos.CENTER);

        HBox topRow = new HBox(30, nameBlock, categoryLabel , categoryArea);
        topRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryArea, Priority.ALWAYS);

        VBox header = new VBox(topRow);
        header.getStyleClass().add("app-header");
        header.setPadding(new Insets(20));

        return header;
    }

    private ScrollPane createMenu() {

        VBox menuBox = new VBox(15);
        menuBox.getStyleClass().add("app-sidebar");

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
        exit.getStyleClass().add("exit-item");

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #333c4c");
        divider.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(divider, new Insets(6, 8, 6, 8));

        Label dashboard = new Label("DASHBOARD");
        dashboard.getStyleClass().add("nav-section-label");
        dashboard.setCursor(Cursor.HAND);

        dashboard.setOnMouseClicked(mouseEvent -> {
            categoryArea.getChildren().clear();
            categoryLabel.setText("");
            showWelcomePage();
            setActiveButton(dashboard);
        });


        menuBox.getChildren().addAll(
                dashboard,
                createNavSection("CUSTOMERS", showCustomers, createCustomer),
                createNavSection("VEHICLES", showVehicles, createVehicle),
                createNavSection("BOOKINGS", showBookings, createBooking),
                createNavSection("WORKORDERS", showWorkOrders, createWorkOrder, startWorkOrder, completeWorkOrder),
                createNavSection("SERVICE", showServices, showMechanics),
                createNavSection("INVOICE", showInvoices, createInvoice, showPayments, processPayment),
                divider,
                exit
        );


        /*
         * Actions för menyknapparna.
         */

        showCustomers.setOnAction(event -> {
            setActiveButton(showCustomers);
            contentPane.getChildren().setAll(new CustomerView());
        });

        createCustomer.setOnAction(event -> {
            setActiveButton(createCustomer);
            contentPane.getChildren().setAll(new CreateCustomerView());
        });

        showVehicles.setOnAction(event -> {
            setActiveButton(showVehicles);
            contentPane.getChildren().setAll(ShowVehicleView.build());
        });

        createVehicle.setOnAction(event -> {
            setActiveButton(createVehicle);
            contentPane.getChildren().setAll(CreateVehicleView.build());
        });

        showBookings.setOnAction(event -> {
            setActiveButton(showBookings);
            ShowBookingsView bookingsView = new ShowBookingsView();
            contentPane.getChildren().setAll(bookingsView.getView());
        });

        createBooking.setOnAction(event -> {
            setActiveButton(createBooking);
            CreateBookingView createBookingView = new CreateBookingView();
            contentPane.getChildren().setAll(createBookingView.getView());
        });

        showServices.setOnAction(event -> {
            setActiveButton(showServices);
            contentPane.getChildren().setAll(new ServiceView());
        });

        showMechanics.setOnAction(event -> {
            setActiveButton(showMechanics);
            contentPane.getChildren().setAll(new MechanicView());
        });

        showWorkOrders.setOnAction(event -> {
            setActiveButton(showWorkOrders);
            ShowWorkOrdersView workOrdersView = new ShowWorkOrdersView();
            contentPane.getChildren().setAll(workOrdersView.getView());
        });

        createWorkOrder.setOnAction(event -> {
            setActiveButton(createWorkOrder);
            contentPane.getChildren().setAll(new CreateWorkOrderView());
        });

        startWorkOrder.setOnAction(event -> {
            setActiveButton(startWorkOrder);
            contentPane.getChildren().setAll(new StartWorkOrderView());
        });

        completeWorkOrder.setOnAction(event -> {
            setActiveButton(completeWorkOrder);
            contentPane.getChildren().setAll(CompleteWorkOrderView.build());
        });

        showInvoices.setOnAction(event -> {
            setActiveButton(showInvoices);
            contentPane.getChildren().setAll(ShowInvoiceView.build());
        });

        createInvoice.setOnAction(event -> {
            setActiveButton(createInvoice);
            contentPane.getChildren().setAll(CreateInvoiceView.build());
        });

        showPayments.setOnAction(event -> {
            setActiveButton(showPayments);
            ShowPaymentsView paymentsView = new ShowPaymentsView();
            contentPane.getChildren().setAll(paymentsView.getView());
        });

        processPayment.setOnAction(event -> {
            setActiveButton(processPayment);
            ProcessPaymentView paymentView = new ProcessPaymentView();
            contentPane.getChildren().setAll(paymentView.getView());
        });

        exit.setOnAction(event ->
                System.exit(0));


        ScrollPane scrollPane = new ScrollPane(menuBox);
        scrollPane.getStyleClass().add("app-sidebar");

        scrollPane.setFitToWidth(true);

        return scrollPane;
    }

    private Button createMenuButton(String text) {

        Button button = new Button(text);

        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(40);

        return button;
    }

    private VBox createNavSection(String sectionTitle, Button... buttons){
        Label label = new Label(sectionTitle);
        label.getStyleClass().add("nav-section-label");
        label.setCursor(Cursor.HAND);

        label.setOnMouseClicked(mouseEvent -> {
            categoryArea.getChildren().setAll(buttons);
            categoryLabel.setText(sectionTitle);
            setActiveButton(label);

        });



        VBox section = new VBox(label);


        return section;
    }

    private void showWelcomePage() {
        //--- Visar dashboarden ---
        contentPane.getChildren().setAll(new DashboardView());
    }

    private void showPage(String pageName) {

        Label pageTitle = new Label(pageName);

        pageTitle.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        contentPane.getChildren().setAll(pageTitle);
    }

    private void setActiveButton(Labeled item){
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