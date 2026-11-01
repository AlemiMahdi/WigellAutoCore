package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Customer;
import com.wac.autocore.service.CustomerService;

@Component 
public class SecuredCustomerActions {
    
    private final CustomerService customerService;

    public SecuredCustomerActions( CustomerService customerService){
        this.customerService = customerService;
    }

    //Alla inloggade användare får läsa kundinformation
    @PreAuthorize ("isAuthenticated()")
    public List<Customer> findAll() {
        return customerService.findAll();
    }

    //Endast ADMIN och WORKSHOP få registrera kunder
    @PreAuthorize ("hasAnyRole('ADMIN', 'WORKSHOP')")
    public Customer createCustomer(
        String name,
        String phone,
        String email
    ){
        return customerService.createCustomer(name, phone, email);
    }


}
