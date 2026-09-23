package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.CustomerEntity;

public class CustomerStorageTest {

    public static void main(String[] args) {
        CustomerRepository repository = new CustomerRepository();

        try {
            if (args.length > 0 && "save".equals(args[0])) {
                if (!repository.findAll().isEmpty()) {
                    System.out.println(
                            "Testet kräver en tom kundtabell. Ingen data ändrades."
                    );
                    return;
                }

                CustomerEntity customer = new CustomerEntity();
                customer.setId(1);
                customer.setName("Testkund");
                customer.setPhone("0701234567");
                customer.setEmail("test@example.com");
                customer.setVip(false);

                repository.save(customer);
                System.out.println("Testkunden har sparats.");
            }

            for (CustomerEntity customer : repository.findAll()) {
                System.out.println(
                        customer.getId() + " - " + customer.getName()
                                + " - " + customer.getEmail()
                );
            }
        } finally {
            HibernateUtil.shutDown();
        }
    }
}