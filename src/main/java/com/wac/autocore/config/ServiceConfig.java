package com.wac.autocore.config;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.*;
import com.wac.autocore.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ServiceConfig {

    @Bean
    public CustomerService customerService(ICustomerRepository customerRepository) {
        return new CustomerService(customerRepository);
    }

    @Bean
    public BookingService bookingService(IBookingRepository bookingRepository) {
        return new BookingService(bookingRepository);
    }
    @Bean
    public InvoiceService invoiceService(IInvoiceRepository invoiceRepository, IWorkOrderRepository workOrderRepository,
                                         IBookingRepository bookingRepository, IVehicleRepository vehicleRepository,
                                         ICustomerRepository customerRepository,
                                         IServiceItemRepository serviceItemRepository) {
        return new InvoiceService(invoiceRepository,
                workOrderRepository::findAll,
                bookingRepository::findAll,
                vehicleRepository::findAll,
                customerRepository::findAll,
                serviceItemRepository::findAll);
    }

    @Bean
    public VehicleService vehicleService(IVehicleRepository vehicleRepository) {
        return new VehicleService(vehicleRepository);
    }

    @Bean
    public MechanicService mechanicService(IMechanicRepository mechanicRepository, IBookingRepository bookingRepository,
                                           IWorkOrderRepository workOrderRepository) {
        return new MechanicService(mechanicRepository,
                bookingRepository::findAll,
                workOrderRepository::findAll);
    }

    @Bean
    public ServiceItemService serviceItemService(IServiceItemRepository serviceItemRepository) {
        return new ServiceItemService(serviceItemRepository);
    }

    @Bean
    public ServicePackageService servicePackageService(IServicePackageRepository servicePackageRepository) {
        return new ServicePackageService(servicePackageRepository);
    }

    @Bean
    public PaymentService paymentService(IPaymentRepository paymentRepository) {
        return new PaymentService(paymentRepository);
    }

    @Bean
    public WorkOrderService workOrderService(IWorkOrderRepository workOrderRepository, IBookingRepository bookingRepository,
                                             IVehicleRepository vehicleRepository,
                                             IServiceItemRepository serviceItemRepository) {
        return new WorkOrderService(workOrderRepository, bookingRepository, serviceItemRepository, vehicleRepository);
    }
}
