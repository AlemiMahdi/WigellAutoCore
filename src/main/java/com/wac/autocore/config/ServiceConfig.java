package com.wac.autocore.config;

import com.wac.autocore.repository.*;
import com.wac.autocore.service.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@Service
public class ServiceConfig {

//    private final GarageSystem garageSystem;
//    private final IWorkOrderRepository workOrderRepository;
//    private final BookingRepository bookingRepository;
//    private final VehicleRepository vehicleRepository;
//    private final InvoiceService invoiceService;
//    private final MechanicService mechanicService;
//    private final IPaymentRepository paymentRepository;
//    private final IServiceItemRepository serviceRepository;
//    private final VehicleService vehicleService;
//    private final ServicePackageService servicePackageService;
//    private final WorkOrderService workOrderService;
//    private final CustomerService customerService;
//    private final BookingService bookingService;
//
//
//        this.garageSystem = new GarageSystem();
//        this.workOrderRepository = new WorkOrderRepository();
//        this.bookingRepository = new BookingRepository();
//        this.invoiceService = new InvoiceService(new InvoiceRepository());
//        this.mechanicService = new MechanicService(new MechanicRepository(), bookingRepository::findAll, workOrderRepository::findAll);
//        this.vehicleRepository = new VehicleRepository();
//        this.vehicleService = new VehicleService(vehicleRepository);
//        this.servicePackageService = new ServicePackageService(new ServicePackageRepository());
//        this.paymentRepository = new PaymentRepository();
//        this.serviceRepository = new ServiceItemRepository();
//        this.workOrderService = new WorkOrderService(workOrderRepository, bookingRepository, serviceRepository, vehicleRepository);
//        this.customerService = new CustomerService(new CustomerRepository());
//        this.bookingService = new BookingService(bookingRepository);
}
