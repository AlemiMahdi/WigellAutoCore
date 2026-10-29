package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.*;

import java.time.LocalDate;

public class GarageSystem {

    public void showCustomers() {
        System.out.println();
        System.out.println("=== CUSTOMERS ===");

        if (Database.getCustomers().isEmpty()) {
            System.out.println("No customers found.");
            return;
        }

        for (Customer customer : Database.getCustomers()) {
            System.out.println(customer);
        }
    }

    public void showVehicles() {
        System.out.println();
        System.out.println("=== VEHICLES ===");

        if (Database.getVehicles().isEmpty()) {
            System.out.println("No vehicles found.");
            return;
        }

        for (Vehicle vehicle : Database.getVehicles()) {
            System.out.println(vehicle);
        }
    }

    public void showBookings() {
        System.out.println();
        System.out.println("=== BOOKINGS ===");

        if (Database.getBookings().isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }

        for (Booking booking : Database.getBookings()) {
            System.out.println(booking);
        }
    }



    public void showMechanics() {
        System.out.println();
        System.out.println("=== MECHANICS ===");

        if (Database.getMechanics().isEmpty()) {
            System.out.println("No mechanics found.");
            return;
        }

        for (Mechanic mechanic : Database.getMechanics()) {
            System.out.println(mechanic);
        }
    }

    public void showWorkOrders() {
        System.out.println();
        System.out.println("=== WORK ORDERS ===");

        if (Database.getWorkOrders().isEmpty()) {
            System.out.println("No work orders found.");
            return;
        }

        for (WorkOrder workOrder : Database.getWorkOrders()) {
            System.out.println(workOrder);
        }
    }

    public void showInvoices() {
        System.out.println();
        System.out.println("=== INVOICES ===");

        if (Database.getInvoices().isEmpty()) {
            System.out.println("No invoices found.");
            return;
        }

        for (Invoice invoice : Database.getInvoices()) {
            System.out.println(invoice);
        }
    }

    public Customer createCustomer(String name, String phone, String email) {
        int id = Database.getCustomers().size() + 1;

        Customer customer = new Customer(id, name, phone, email);
        Database.getCustomers().add(customer);

        System.out.println("Customer created successfully.");
        System.out.println(customer);

        return customer;
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {

        Customer customer = findCustomer(customerId);

        if (customer == null) {
            System.out.println("Customer with ID " + customerId + " does not exist.");
            return null;
        }

        Vehicle vehicle = new Vehicle(
                registrationNumber,
                brand,
                model,
                year,
                customerId
        );

        Database.getVehicles().add(vehicle);

        System.out.println("Vehicle created successfully.");
        System.out.println(vehicle);

        return vehicle;
    }

    public Booking createBooking(int vehicleId,
                                 LocalDate date,
                                 String description) {

        Vehicle vehicle = findVehicle(vehicleId);

        if (vehicle == null) {
            System.out.println("Vehicle with ID " + vehicleId + " does not exist.");
            return null;
        }

        Booking booking = new Booking(
                vehicleId,
                date,
                description
        );

        Database.getBookings().add(booking);

        System.out.println("Booking created successfully.");
        System.out.println(booking);

        return booking;
    }

    //Konsol app gammal metod, no longer in use!
//    public WorkOrder createWorkOrder(WorkOrderDto dto, WorkOrderTypeEnum type) {
//
//        Mechanic mechanic = findMechanic(dto.getMechanicId());
//        if (mechanic == null) {
//            System.out.println("Mechanic with ID " + dto.getMechanicId() + " does not exist or is not available.");
//            return null;
//        }
//
//        WorkOrderTypeManager manager = new WorkOrderTypeManager();
//        manager.setStrategy(type);
//        WorkOrder workOrder = manager.createWorkOrderType(dto, language);
//        if (workOrder == null) {
//            System.out.println("WorkOrder does not exist.");
//            return null;
//        }
//
//        Booking booking = findBooking(workOrder.getBookingId());
//        if (booking != null && type != WorkOrderTypeEnum.COMPLAINT) {
//            booking.setStatus("WORK_ORDER_CREATED");
//        }
//
//        int id = Database.getWorkOrders().stream()
//        .mapToInt(WorkOrder::getId)
//        .max()
//        .orElse(0) + 1;
//
//        workOrder.setId(id);
//
//        System.out.println("Work order created successfully.");
//        System.out.println(workOrder);
//
//        Database.getWorkOrders().add(workOrder);
//        return workOrder;
//    }

//    public List<String> validateWorkOrderForConfirmation(int workOrderId) {
//        List<String> errors = new ArrayList<>();
//        WorkOrder workOrder = findWorkOrder(workOrderId);
//
//        if (workOrder == null) {
//            errors.add("confirmWorkOrder.orderMissing");
//            return errors;
//        }
//
//        if (workOrder.getType() == null) {
//            errors.add("confirmWorkOrder.typeMissing");
//        }
//
//        if (workOrder.getType() == WorkOrderTypeEnum.COMPLAINT) {
//            Integer originalId = workOrder.getOriginalWorkOrderId();
//
//            if (originalId == null) {
//                errors.add("confirmWorkOrder.originalRequired");
//            } else if (originalId == workOrder.getId()) {
//                errors.add("confirmWorkOrder.originalSelfReference");
//            } else {
//                WorkOrder original = findWorkOrder(originalId);
//
//                if (original == null) {
//                    errors.add("confirmWorkOrder.originalMissing");
//                } else if (original.getStatus() != WorkOrderStatus.COMPLETED) {
//                    errors.add("confirmWorkOrder.originalNotCompleted");
//                }
//            }
//        }
//
//        Booking booking = findBooking(workOrder.getBookingId());
//
//        if (booking == null) {
//            errors.add("confirmWorkOrder.bookingMissing");
//        } else {
//            Vehicle vehicle = findVehicle(booking.getVehicleId());
//
//            if (vehicle == null) {
//                errors.add("confirmWorkOrder.vehicleMissing");
//            } else if (findCustomer(vehicle.getCustomerId()) == null) {
//                errors.add("confirmWorkOrder.customerMissing");
//            }
//        }
//
//        if (findMechanic(workOrder.getMechanicId()) == null) {
//            errors.add("confirmWorkOrder.mechanicMissing");
//        }
//
//        List<Integer> serviceIds = workOrder.getServiceItemIds();
//
//        if (serviceIds == null || serviceIds.isEmpty()) {
//            errors.add("confirmWorkOrder.servicesMissing");
//        } else {
//            for (Integer serviceId : serviceIds) {
//                boolean exists = serviceId != null
//                        && Database.getServiceItems().stream()
//                        .anyMatch(service -> service.getId() == serviceId);
//
//                if (!exists) {
//                    errors.add("confirmWorkOrder.serviceMissing");
//                    break;
//                }
//            }
//        }
//
//        return errors;
//    }

//    // Samma logik som tidigare: starta via GarageSystem och spara via repositories.
//    private void startSelectedWorkOrder() {
//
//        feedbackLabel.textProperty().unbind();
//        feedbackLabel.setText("");
//
//        WorkOrder workorder = selectedWorkOrder;
//        if (workorder == null) {
//            UiKit.showError(feedbackLabel, language.text("startWorkOrder.select").get());
//            return;
//        }
//
//        List<String> errors =
//                garageSystem.validateWorkOrderForConfirmation(workorder.getId());
//
//        if (!errors.isEmpty()) {
//            StringExpression message = language.text(errors.get(0));
//
//            for (int i = 1; i < errors.size(); i++) {
//                message = message.concat("\n").concat(
//                        language.text(errors.get(i))
//                );
//            }
//
//            feedbackLabel.setWrapText(true);
//            UiKit.showError(feedbackLabel, "");
//            feedbackLabel.textProperty().bind(message);
//            return;
//        }
//
//        // Sparar den valda arbetsorderns status innan vi försöker starta den.
//        WorkOrderStatus previousStatus = workorder.getStatus();
//
//        garageSystem.startWorkOrder(workorder.getId());
//
//        if (previousStatus == WorkOrderStatus.CONFIRMED
//                && workorder.getStatus() == WorkOrderStatus.IN_PROGRESS) {
//            // Hämtar bokningen och mekanikern som startWorkOrder har ändrat.
//            Booking booking = findBooking(workorder);
//            Mechanic mechanic = findMechanic(workorder);
//
//            try {
//                // Sparar arbetsorderns, bokningens och mekanikerns nya status.
//                workOrderRepository.save(workorder);
//
//                if (booking != null) {
//                    bookingRepository.save(booking);
//                }
//
//                if (mechanic != null) {
//                    mechanicRepository.save(mechanic);
//                }
//
//                UiKit.showSuccess(feedbackLabel,
//                        language.text("startWorkOrder.success").get() + " " + workorder.getId());
//            } catch (RuntimeException exception) {
//                UiKit.showError(feedbackLabel,
//                        language.text("startWorkOrder.saveError").get());
//                exception.printStackTrace();
//            }
//
//        } else {
//            UiKit.showError(feedbackLabel, language.text("startWorkOrder.cannotStart").get());
//        }
//
//        // Återställer valet och laddar om listan (startad order försvinner ur den).
//        // Inget kort väljs automatiskt, så man startar inte nästa order av misstag.
//        reloadList(false);
//    }

//    public void startWorkOrder(int workOrderId) {
//        WorkOrder workOrder = findWorkOrder(workOrderId);
//
//        if (workOrder == null) {
//            System.out.println("Work order with ID " + workOrderId + " does not exist.");
//            return;
//        }
//
//        if (!workOrder.getStatus().canChangeTo(WorkOrderStatus.IN_PROGRESS)) {
//            System.out.println("Work order cannot be started.");
//            return;
//        }
//
//        List<String> errors = validateWorkOrderForConfirmation(workOrderId);
//
//        if (!errors.isEmpty()) {
//            System.out.println(
//                    "Work order cannot be started: " + String.join(", ", errors)
//            );
//            return;
//        }
//
//        Mechanic mechanic = findMechanic(workOrder.getMechanicId());
//        Booking booking = findBooking(workOrder.getBookingId());
//
//        if (mechanic == null || !mechanic.isAvailable()) {
//            System.out.println("Cannot start work order: mechanic is missing or unavailable.");
//            return;
//        }
//
//        if (booking == null) {
//            System.out.println("Cannot start work order: booking is missing.");
//            return;
//        }
//
//        // Reklamationer ska inte ändra originalbokningens status.
//        if (!sharesOriginalBooking(workOrder)) {
//            booking.setStatus("IN_PROGRESS");
//        }
//
//        mechanic.setAvailable(false);
//
//        workOrder.changeStatus(WorkOrderStatus.IN_PROGRESS);
//
//        System.out.println("Work order " + workOrderId + " has been started.");
//    }



    //refaktoriserad till InvoiceService

    /*
    public Invoice createInvoice(
            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        Invoice invoice = prepareInvoice(
                workOrderId,
                discountCode,
                extraDescription,
                extraCost
        );

        if (invoice != null) {
            Database.getInvoices().add(invoice);
        }

        return invoice;
    }

     */

    /*
    public Invoice prepareInvoice(
            int workOrderId, 
            String discountCode,
            String extraDescription,
            double extraCost) {
        WorkOrder workOrder = findWorkOrder(workOrderId);

        if (workOrder == null) {
            System.out.println("Work order with ID " + workOrderId + " does not exist.");
            return null;
        }
        boolean alreadyInvoiced = Database.getInvoices()
        .stream()
        .anyMatch(invoice -> invoice.getWorkOrderId() == workOrderId);

        if (alreadyInvoiced) {
            System.out.println(
                "Work order " + workOrderId + " already has an invoice."
            );
            return null;
        }
        if (!Double.isFinite(extraCost) || extraCost < 0) {
            System.out.println("Extra cost must be a valid non-negative number.");
            return null;
        }

        if (workOrder.getType() == WorkOrderTypeEnum.COMPLAINT
                && extraCost > 0
                && (extraDescription == null || extraDescription.isEmpty())) {
            System.out.println("Extra cost requires a description.");
            return null;
        }

        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            System.out.println("Invoice can only be created for a completed work order.");
            return null;
        }

        Booking booking = findBooking(workOrder.getBookingId());
        if (booking == null) {
            System.out.println("Booking for work order " + workOrderId + " does not exist.");
            return null;
        }


            Invoice invoice = new Invoice(
                    workOrderId,
                    LocalDate.now(),
                    0.0
            );

            boolean isComplaint = workOrder.getType() == WorkOrderTypeEnum.COMPLAINT;

        if (isComplaint) {
            for (Integer serviceId : workOrder.getServiceItemIds()) {
                ServiceItem service = Database.getServiceItems().stream()
                        .filter(item -> Integer.valueOf(item.getId()).equals(serviceId))
                        .findFirst()
                        .orElse(null);

                if (service == null) {
                    System.out.println(
                            "Cannot create invoice: service missing, ID " + serviceId
                    );
                    return null;
                }

                invoice.addLine(new InvoiceLine(
                        service.getId(),
                        service.getName(),
                        0.0
                ));
            }
        } else {
            for (InvoiceLine frozenLine : booking.getFrozenPrice()) {
                invoice.addLine(new InvoiceLine(
                        frozenLine.getServiceItemId(),
                        frozenLine.getServiceName(),
                        frozenLine.getPrice()
                ));
            }
        }

            if (isComplaint && extraCost > 0) {
                InvoiceLine extraLine = new InvoiceLine(
                    0,
                    extraDescription,
                    extraCost
                );

                invoice.addLine(extraLine);
            }

            double amount = invoice.getAmount();
            double discount = 0.0;

            Vehicle vehicle = findVehicle(booking.getVehicleId());
            if (vehicle != null) {
                Customer customer = findCustomer(vehicle.getCustomerId());

                if (customer != null && customer.isVip()) {
                    discount += amount * 0.10;
                    System.out.println("VIP discount applied: 10%");
            }
        }

        if (discountCode != null && !discountCode.trim().isEmpty()) {

            if (discountCode.equalsIgnoreCase("WELCOME10")) {
                discount += amount * 0.10;
                System.out.println("Discount code WELCOME10 applied.");

            } else if (discountCode.equalsIgnoreCase("SERVICE200")) {
                discount += 200.0;
                System.out.println("Discount code SERVICE200 applied.");

            } else {
                System.out.println("Unknown discount code. No code discount applied.");
            }
        }

        if (discount > amount) {
            discount = amount;
        }

        invoice.setDiscount(discount);
        invoice.distributeDiscountToLines();

        return invoice;
    }
    */

//    private boolean sharesOriginalBooking(WorkOrder workOrder) {
//        if (workOrder.getType() != WorkOrderTypeEnum.COMPLAINT
//                || workOrder.getOriginalWorkOrderId() == null) {
//            return false;
//        }
//
//        WorkOrder original = findWorkOrder(
//                workOrder.getOriginalWorkOrderId()
//        );
//
//        return original != null
//                && original.getBookingId() == workOrder.getBookingId();
//    }

    private Customer findCustomer(int id) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == id) {
                return customer;
            }
        }

        return null;
    }

    private Vehicle findVehicle(int id) {
        for (Vehicle vehicle : Database.getVehicles()) {
            if (vehicle.getId() == id) {
                return vehicle;
            }
        }

        return null;
    }

//    private Booking findBooking(int id) {
//        for (Booking booking : Database.getBookings()) {
//            if (booking.getId() == id) {
//                return booking;
//            }
//        }
//
//        return null;
//    }

//    private Mechanic findMechanic(int id) {
//        for (Mechanic mechanic : Database.getMechanics()) {
//            if (mechanic.getId() == id) {
//                return mechanic;
//            }
//        }
//
//        return null;
//    }

//    static Invoice findInvoice(int id) {
//        for (Invoice invoice : Database.getInvoices()) {
//            if (invoice.getId() == id) {
//                return invoice;
//            }
//        }
//
//        return null;
//    }

//    private WorkOrder findWorkOrder(int id) {
//        for (WorkOrder workOrder : Database.getWorkOrders()) {
//            if (workOrder.getId() == id) {
//                return workOrder;
//            }
//        }
//
//        return null;
//    }
}