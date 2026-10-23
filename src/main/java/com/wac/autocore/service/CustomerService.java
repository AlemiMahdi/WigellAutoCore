package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.ICustomerRepository;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.DataAccessException;

public class CustomerService {

    private final ICustomerRepository customerRepository;

    public CustomerService(ICustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer( String name, String phone, String email){

        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("customer.create.nameRequired");
        }

        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("customer.create.phoneRequired");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("customer.create.emailRequired");
        }

        Customer customer = new Customer();

        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);

        try {
            customerRepository.save(customer);
        } catch (RuntimeException exception) {
            throw new DataAccessException(
                    "customer.create.error", exception
            );
        }

        Database.getCustomers().add(customer);
        return customer;
    }

}
