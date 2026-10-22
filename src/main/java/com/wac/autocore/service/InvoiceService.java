package com.wac.autocore.service;

import com.wac.autocore.model.*;
import com.wac.autocore.repository.IInvoiceRepository;
import com.wac.autocore.data.Database;
import com.wac.autocore.exception.InvoiceValidationException;
import com.wac.autocore.model.WorkOrderTypeEnum;
import java.util.function.Supplier;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.repository.VehicleRepository;
import com.wac.autocore.repository.CustomerRepository;
import com.wac.autocore.repository.ServiceItemRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

public class InvoiceService {

    private final IInvoiceRepository invoiceRepository;
    private final Supplier<List<WorkOrder>> workOrderSource;
    private final Supplier<List<Booking>> bookingSource;
    private final Supplier<List<Vehicle>> vehicleSource;
    private final Supplier<List<Customer>> customerSource;
    private final Supplier<List<ServiceItem>> serviceItemSource;

//Korta konstruktorn ställer till det för Spring eventuellt, ska inte skapa NEW instanser
    public InvoiceService(IInvoiceRepository invoiceRepository) {
        this(
                invoiceRepository,
                new WorkOrderRepository()::findAll,
                new BookingRepository()::findAll,
                new VehicleRepository()::findAll,
                new CustomerRepository()::findAll,
                new ServiceItemRepository()::findAll
        );
    }

    public InvoiceService(
            IInvoiceRepository invoiceRepository,
            Supplier<List<WorkOrder>> workOrderSource,
            Supplier<List<Booking>> bookingSource,
            Supplier<List<Vehicle>> vehicleSource,
            Supplier<List<Customer>> customerSource,
            Supplier<List<ServiceItem>> serviceItemSource) {

        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
        this.workOrderSource = Objects.requireNonNull(workOrderSource);
        this.bookingSource = Objects.requireNonNull(bookingSource);
        this.vehicleSource = Objects.requireNonNull(vehicleSource);
        this.customerSource = Objects.requireNonNull(customerSource);
        this.serviceItemSource = Objects.requireNonNull(serviceItemSource);
    }

//    public InvoiceService(
//            IInvoiceRepository invoiceRepository,
//            WorkOrderRepository workOrderRepository,
//            BookingRepository bookingRepository,
//            VehicleRepository vehicleRepository,
//            CustomerRepository customerRepository,
//            ServiceItemRepository serviceItemRepository
//    ){
//        this.invoiceRepository = Objects.requireNonNull(invoiceRepository);
//        this.workOrderSource = workOrderRepository::findAll;
//        this.bookingSource = bookingRepository::findAll;
//        this.vehicleSource = vehicleRepository::findAll;
//        this.customerSource = customerRepository::findAll;
//        this.serviceItemSource = serviceItemRepository::findAll;
//    }

    public Invoice createInvoice(



            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        validateExtraCost(extraDescription, extraCost);

        Invoice invoice = prepareInvoice(
                workOrderId,
                discountCode,
                extraDescription,
                extraCost
        );

        if (invoice == null) {
            return null;
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);
        Database.getInvoices().add(savedInvoice);

        return savedInvoice;
    }

    public Invoice previewInvoice(
            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        validateExtraCost(extraDescription, extraCost);

        return prepareInvoice(
                workOrderId,
                discountCode,
                extraDescription,
                extraCost
        );
    }

    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    public void save(Invoice invoice) {
        Objects.requireNonNull(invoice, "Invoice is required");
        invoiceRepository.save(invoice);
    }

    private void validateExtraCost(
            String extraDescription,
            double extraCost) {

        if (!Double.isFinite(extraCost) || extraCost < 0) {
            throw new InvoiceValidationException(
                    "createInvoice.invalidExtraCost"
            );
        }

        try {
            BigDecimal.valueOf(extraCost)
                    .setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvoiceValidationException(
                    "createInvoice.invalidExtraCost", exception
            );
        }

        if (extraCost > 0
                && (extraDescription == null
                || extraDescription.trim().isEmpty())) {
            throw new InvoiceValidationException(
                    "createInvoice.missingExtraDescription"
            );
        }
    }

    public List<Invoice> getLoadedInvoices() {
        return invoiceRepository.findAll();
    }

    public List<WorkOrder> getInvoiceableWorkOrders() {

        List<Invoice> invoices = invoiceRepository.findAll();

        return workOrderSource.get().stream()
                .filter(order -> order.getStatus() == WorkOrderStatus.COMPLETED)
                .filter(order -> invoices.stream()
                        .noneMatch(invoice ->
                                invoice.getWorkOrderId() == order.getId()))
                .collect(Collectors.toList());
    }

    public Vehicle findVehicleForWorkOrder(int workOrderId) {
        WorkOrder order = workOrderSource.get().stream()
                .filter(item -> item.getId() == workOrderId)
                .findFirst()
                .orElse(null);

        if (order == null) {
            return null;
        }

        Booking booking = bookingSource.get().stream()
                .filter(item -> item.getId() == order.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            return null;
        }

        return vehicleSource.get().stream()
                .filter(item -> item.getId() == booking.getVehicleId())
                .findFirst()
                .orElse(null);
    }

    private void calculateAmountFromLines(Invoice invoice) {
        double sum = 0.0;

        for (InvoiceLine line : invoice.getLines()) {
            sum += line.getPrice();
        }

        invoice.setAmount(sum);
    }

    private void distributeDiscountToLines(Invoice invoice) {
        List<InvoiceLine> lines = invoice.getLines();

        if (lines.isEmpty()) {
            return;
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (InvoiceLine line : lines) {
            BigDecimal price = BigDecimal
                    .valueOf(line.getPrice())
                    .setScale(2, RoundingMode.UNNECESSARY);

            if (price.signum() < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }

            totalPrice = totalPrice.add(price);
        }

        BigDecimal totalDiscount = BigDecimal
                .valueOf(invoice.getDiscount())
                .setScale(2, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(totalPrice);

        BigDecimal accumulatedPrice = BigDecimal.ZERO;
        BigDecimal allocatedDiscount = BigDecimal.ZERO;

        for (InvoiceLine line : lines) {
            accumulatedPrice = accumulatedPrice.add(
                    BigDecimal.valueOf(line.getPrice())
            );

            BigDecimal accumulatedDiscount =
                    totalPrice.signum() == 0
                            ? BigDecimal.ZERO
                            : totalDiscount
                            .multiply(accumulatedPrice)
                            .divide(
                                    totalPrice,
                                    2,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal lineDiscount =
                    accumulatedDiscount.subtract(allocatedDiscount);

            line.setDiscount(lineDiscount.doubleValue());

            allocatedDiscount = accumulatedDiscount;
        }

        invoice.setAmount(totalPrice.doubleValue());
        invoice.setDiscount(totalDiscount.doubleValue());
    }

    public Invoice prepareInvoice(
            int workOrderId,
            String discountCode,
            String extraDescription,
            double extraCost) {

        validateExtraCost(extraDescription, extraCost);

        WorkOrder workOrder = workOrderSource.get().stream()
                .filter(order -> order.getId() == workOrderId)
                .findFirst()
                .orElse(null);

        if (workOrder == null) {
            throw new InvoiceValidationException(
                    "createInvoice.workOrderMissing"
            );
        }
        boolean alreadyInvoiced = invoiceRepository.findAll()
                .stream()
                .anyMatch(invoice -> invoice.getWorkOrderId() == workOrderId);

        if (alreadyInvoiced) {
            throw new InvoiceValidationException(
                    "createInvoice.alreadyInvoiced"
            );
        }

        if (workOrder.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new InvoiceValidationException(
                    "createInvoice.workOrderNotCompleted"
            );
        }

        Booking booking = bookingSource.get().stream()
                .filter(b -> b.getId() == workOrder.getBookingId())
                .findFirst()
                .orElse(null);

        if (booking == null) {
            throw new InvoiceValidationException(
                    "createInvoice.bookingMissing"
            );
        }


        Invoice invoice = new Invoice(
                workOrderId,
                LocalDate.now(),
                0.0
        );

        boolean isComplaint = workOrder.getType() == WorkOrderTypeEnum.COMPLAINT;

        if (!isComplaint
                && (booking.getFrozenPrice() == null
                || booking.getFrozenPrice().isEmpty())) {
            throw new InvoiceValidationException(
                    "createInvoice.missingPriceHistory"
            );
        }

        if (isComplaint) {
            List<ServiceItem> services = serviceItemSource.get();

            for (Integer serviceId : workOrder.getServiceItemIds()) {
                ServiceItem service = services.stream()
                        .filter(item -> Integer.valueOf(item.getId()).equals(serviceId))
                        .findFirst()
                        .orElse(null);

                if (service == null) {
                    throw new InvoiceValidationException(
                            "createInvoice.serviceMissing"
                    );
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

        /*
        System.out.println(
                "WO=" + workOrder.getId()
                        + ", typ=" + workOrder.getType()
                        + ", bokning=" + booking.getId()
                        + ", sparade prisrader=" + booking.getFrozenPrice().size()
                        + ", fakturarader=" + invoice.getLines().size()
        );

        for (InvoiceLine line : invoice.getLines()) {
            System.out.println(
                    "Tjänst=" + line.getServiceName()
                            + ", pris=" + line.getPrice()
            );
        }
*/
        calculateAmountFromLines(invoice);

        double amount = invoice.getAmount();
        double discount = 0.0;

        Vehicle vehicle = vehicleSource.get().stream()
                .filter(v -> v.getId() == booking.getVehicleId())
                .findFirst()
                .orElse(null);

        if (vehicle != null) {
            Customer customer = customerSource.get().stream()
                    .filter(c -> c.getId() == vehicle.getCustomerId())
                    .findFirst()
                    .orElse(null);

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
        distributeDiscountToLines(invoice);

        return invoice;
    }

}