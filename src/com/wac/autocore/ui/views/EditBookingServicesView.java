package com.wac.autocore.ui.views;
import com.wac.autocore.data.Database;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.Locale;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.ui.UiKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EditBookingServicesView {

    public VBox getView() {
        return getView(null);
    }

    public VBox getView(Booking selectedBooking) {
        LanguageManager language = LanguageManager.getInstance();

        Label titleLabel = new Label();

        titleLabel.textProperty().bind(language.text("editBookingServices.title"));
        titleLabel.getStyleClass().add("panel-title");

        Label bookingLabel = new Label();
        bookingLabel.textProperty().bind(language.text("editBookingServices.booking"));

        ComboBox<Booking> bookingComboBox = new ComboBox<>(
                FXCollections.observableArrayList(Database.getBookings())
        );

        bookingComboBox.setMaxWidth(Double.MAX_VALUE);
        bookingComboBox.promptTextProperty().bind(language.text("editBookingServices.selectBooking"));


        // Visar bokningens ID och datum utan modellens engelska toString-text.
        bookingComboBox.setCellFactory(list -> createBookingCell());
        bookingComboBox.setButtonCell(createBookingCell());

        Label servicesLabel = new Label();
        servicesLabel.textProperty().bind(
                language.text("editBookingServices.services")
        );

        ListView<ServiceItem> serviceList = new ListView<>(
                FXCollections.observableArrayList(Database.getServiceItems())
        );
        serviceList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        serviceList.setPrefHeight(250);
        serviceList.disableProperty().bind(
                bookingComboBox.valueProperty().isNull()
        );

        serviceList.setCellFactory(list -> {
            ListCell<ServiceItem> cell = new ListCell<ServiceItem>() {
                @Override
                protected void updateItem(ServiceItem service, boolean empty) {
                    super.updateItem(service, empty);

                    setText(empty || service == null
                            ? null
                            : service.getId() + " – " + service.getName());
                }
            };

            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                if (event.getButton() == MouseButton.PRIMARY && !cell.isEmpty()) {
                    int index = cell.getIndex();

                    if (serviceList.getSelectionModel().isSelected(index)) {
                        serviceList.getSelectionModel().clearSelection(index);
                    } else {
                        serviceList.getSelectionModel().select(index);
                    }

                    serviceList.requestFocus();
                    event.consume();
                }
            });

            return cell;
        });

        Label emptyLabel = new Label();
        emptyLabel.textProperty().bind(
                language.text("editBookingServices.empty")
        );
        serviceList.setPlaceholder(emptyLabel);

        Label helpLabel = new Label();
        helpLabel.textProperty().bind(language.text("editBookingServices.help"));
        helpLabel.setWrapText(true);

        Label totalTimeLabel = new Label();
        Label totalPriceLabel = new Label();

        Runnable updateTotals = () -> {
            int totalMinutes = serviceList.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .mapToInt(ServiceItem::getEstimatedMinutes)
                    .sum();

            double totalPrice = serviceList.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .mapToDouble(ServiceItem::getPrice)
                    .sum();

            totalTimeLabel.textProperty().bind(
                    language.text("editBookingServices.totalTime")
                            .concat(" ")
                            .concat(String.valueOf(totalMinutes))
                            .concat(" min")
            );

            totalPriceLabel.textProperty().bind(
                    language.text("editBookingServices.totalPrice")
                            .concat(" ")
                            .concat(String.format(
                                    Locale.ROOT, "%.2f SEK", totalPrice
                            ))
            );
        };

        serviceList.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<ServiceItem>) change -> updateTotals.run()
        );
// Markerar de tjänster som redan hör till bokningen.
        bookingComboBox.valueProperty().addListener(
                (observable, previous, selected) -> {
                    serviceList.getSelectionModel().clearSelection();

                    if (selected == null) {
                        return;
                    }

                    for (int i = 0; i < serviceList.getItems().size(); i++) {
                        ServiceItem service = serviceList.getItems().get(i);

                        boolean belongsToBooking = selected.getServices()
                                .stream()
                                .anyMatch(savedService ->
                                        savedService.getId() == service.getId());

                        if (belongsToBooking) {
                            serviceList.getSelectionModel().select(i);
                        }
                    }
                }
        );

        updateTotals.run();

        Label noticeLabel = UiKit.feedbackLabel();

        BookingRepository bookingRepository = new BookingRepository();

        Button saveButton = UiKit.primaryButton("");
        saveButton.textProperty().bind(language.text("editBookingServices.save"));
        saveButton.disableProperty().bind(
                bookingComboBox.valueProperty().isNull()
        );

        saveButton.setOnAction(event -> {
            Booking booking = bookingComboBox.getValue();

            if (booking == null) {
                return;
            }

            // Kopierar valet så att listan inte följer senare klick i vyn
            List<ServiceItem> selectedServices = new ArrayList<>(
                    serviceList.getSelectionModel().getSelectedItems()
            );

            List<Integer> serviceIds = new ArrayList<>();

            for (ServiceItem service : selectedServices) {
                serviceIds.add(service.getId());
            }

            final int totalMinutes;

            try {
                totalMinutes = bookingRepository.updateBookingServices(
                        booking.getId(), serviceIds
                );
            } catch (RuntimeException exception) {
                String key = "editBookingServices.saveError";

                List<String> validationKeys = Arrays.asList(
                        "editBookingServices.notFound",
                        "editBookingServices.locked",
                        "editBookingServices.selectService",
                        "editBookingServices.serviceMissing",
                        "editBookingServices.invalidDuration",
                        "editBookingServices.missingTime",
                        "editBookingServices.overlap"
                );

                if (exception instanceof IllegalArgumentException
                        && validationKeys.contains(exception.getMessage())) {
                    key = exception.getMessage();
                } else {
                    java.util.logging.Logger.getLogger(
                            EditBookingServicesView.class.getName()
                    ).log(
                            java.util.logging.Level.SEVERE,
                            "Could not update booking services",
                            exception
                    );
                }

                noticeLabel.textProperty().unbind();
                UiKit.showError(noticeLabel, "");
                noticeLabel.textProperty().bind(language.text(key));
                return;
            }

            // Uppdaterar minnet först efter lyckat databassparande.
            booking.setServices(new ArrayList<>(selectedServices));
            booking.setDurationMinutes(totalMinutes);

            for (WorkOrder order : Database.getWorkOrders()) {
                if (order.getBookingId() == booking.getId()
                        && "CREATED".equals(order.getStatus())) {
                    order.setServiceItemIds(new ArrayList<>(serviceIds));
                }
            }

            noticeLabel.textProperty().unbind();
            UiKit.showSuccess(noticeLabel, "");
            noticeLabel.textProperty().bind(
                    language.text("editBookingServices.saved")
            );
        });

        VBox root = new VBox(
                10,
                titleLabel,
                bookingLabel,
                bookingComboBox,
                servicesLabel,
                helpLabel,
                serviceList,
                totalTimeLabel,
                totalPriceLabel,
                noticeLabel,
                saveButton
        );

        root.setPadding(new Insets(20));
        VBox.setVgrow(serviceList, Priority.ALWAYS);

        // Förväljer bokningen när formuläret öppnas från bokningslistan.
        if (selectedBooking != null) {
            bookingComboBox.setValue(selectedBooking);
            bookingComboBox.setDisable(true);
        }

        return root;
    }

    private ListCell<Booking> createBookingCell() {
        return new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.getId() + " – " + item.getDate());
            }
        };
    }
}
