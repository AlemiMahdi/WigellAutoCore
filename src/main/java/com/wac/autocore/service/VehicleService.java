package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.IVehicleRepository;

import java.util.List;

public class VehicleService {

    private final IVehicleRepository vehicleRepository;

    public VehicleService(IVehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle createVehicle(String registrationNumber,
                                 String brand,
                                 String model,
                                 int year,
                                 int customerId) {

        Customer customer = findCustomer(customerId);

        if (customer == null) {
            throw new IllegalArgumentException("vehicle.create.invalidOwner");
        }

        Vehicle vehicle = new Vehicle(
                registrationNumber,
                brand,
                model,
                year,
                customerId
        );

        vehicleRepository.save(vehicle);
        // Tillfällig, annan kod i GarageSystem läser listan
        Database.getVehicles().add(vehicle);

        return vehicle;
    }

    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    // Byts mot ICustomerRepository när WAC-89 är klar
    private Customer findCustomer(int id) {
        for (Customer customer : Database.getCustomers()) {
            if (customer.getId() == id) {
                return customer;
            }
        }

        return null;
    }

}
