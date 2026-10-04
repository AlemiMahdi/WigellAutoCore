package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Customer;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// Hanterar lagring och hämtning av kunder i databasen.
public class CustomerRepository {

    public void save(Customer customer) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(customer);
                transaction.commit();

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    public List<Customer> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<Customer> customers = session.createQuery(
                        "from Customer order by id",
                        Customer.class
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

    public List<Customer> findAllCustomers() {
        return findAll();
    }
}