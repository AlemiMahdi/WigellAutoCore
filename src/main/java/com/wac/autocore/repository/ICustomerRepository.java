package com.wac.autocore.repository;

import java.util.List;

import com.wac.autocore.model.Customer;

public interface ICustomerRepository {
    
    Customer save(Customer customer);
    List<Customer> findAll();
    
}
