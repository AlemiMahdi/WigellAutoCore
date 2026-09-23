package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.VehicleEntity;

public class VehicleStorageTest {

    public static void main(String[] args) {
        VehicleRepository vehicleRepository = new VehicleRepository();
        CustomerRepository customerRepository = new CustomerRepository();

        try {
            if (args.length > 0 && "save".equals(args[0])) {
                if (!vehicleRepository.findAll().isEmpty()) {
                    System.out.println(
                            "Testet kräver en tom fordonstabell. Ingen data ändrades."
                    );
                    return;
                }

                boolean customerExists = customerRepository.findAll()
                        .stream()
                        .anyMatch(customer -> customer.getId() == 1);

                if (!customerExists) {
                    System.out.println(
                            "Kund med ID 1 saknas. Ingen data ändrades."
                    );
                    return;
                }

                VehicleEntity vehicle = new VehicleEntity();
                vehicle.setId(1);
                vehicle.setRegistrationNumber("TEST123");
                vehicle.setBrand("Volvo");
                vehicle.setModel("V70");
                vehicle.setYear(2012);
                vehicle.setCustomerId(1);

                vehicleRepository.save(vehicle);
                System.out.println("Testfordonet har sparats.");
            }

            for (VehicleEntity vehicle : vehicleRepository.findAll()) {
                System.out.println(
                        vehicle.getId()
                                + " - " + vehicle.getRegistrationNumber()
                                + " - Customer ID: " + vehicle.getCustomerId()
                );
            }
        } finally {
            HibernateUtil.shutDown();
        }
    }
}