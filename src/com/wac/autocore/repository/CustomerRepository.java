package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.CustomerEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.wac.autocore.model.Customer;
import java.util.ArrayList;

import java.util.List;

// Hanterar lagring och hämtning av kunder i databasen.
public class CustomerRepository {

    public void save(CustomerEntity customer) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                // Sparar en ny kund eller uppdaterar kunden med samma ID.
                session.saveOrUpdate(customer);
                transaction.commit();
            } catch (RuntimeException exception) {
                // Ångrar databasändringen om något misslyckas.
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public List<CustomerEntity> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<CustomerEntity> customers = session.createQuery(
                        "from CustomerEntity order by id",
                        CustomerEntity.class
                ).getResultList();

                transaction.commit();
                return customers;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
    // Omvandlar originalets kund till en entity och sparar den.
    public void save(Customer customer) {
        CustomerEntity entity = new CustomerEntity();

        entity.setId(customer.getId());
        entity.setName(customer.getName());
        entity.setPhone(customer.getPhone());
        entity.setEmail(customer.getEmail());
        entity.setVip(customer.isVip());

        save(entity);
    }

    // Hämtar databasens kunder som originalets Customer-objekt.
    public List<Customer> findAllCustomers() {
        List<Customer> customers = new ArrayList<>();

        for (CustomerEntity entity : findAll()) {
            Customer customer = new Customer(
                    entity.getId(),
                    entity.getName(),
                    entity.getPhone(),
                    entity.getEmail()
            );

            // Originalets konstruktor sätter VIP till false.

            // Här återställs kundens sparade VIP-status.
            customer.setVip(entity.isVip());

            customers.add(customer);
        }

        return customers;
    }

}