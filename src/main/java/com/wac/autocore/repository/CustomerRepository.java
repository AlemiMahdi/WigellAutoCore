package com.wac.autocore.repository;

import com.wac.autocore.model.Customer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Hanterar lagring och hämtning av kunder i databasen.
@Repository
@Transactional
public class CustomerRepository implements ICustomerRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Customer save(Customer customer) {
        entityManager.persist(customer);
        return customer;
    }

    @Override
    public List<Customer> findAll() {
        return entityManager.createQuery(
                "from Customer order by id",
                Customer.class
        ).getResultList();
    }
}