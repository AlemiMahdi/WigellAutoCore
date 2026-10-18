package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.ICustomerRepository;

public class CustomerService {

    private final ICustomerRepository customerRepository;

    public CustomerService(ICustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer( String name, String phone, String email){

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("customer.create.nameRequired");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("customer.create.phoneRequired");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("customer.create.emailRequired");
        }

        Customer customer = new Customer();

        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        
        customerRepository.save(customer);

        Database.getCustomers().add(customer);
        return customer;
    }

}
