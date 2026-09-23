package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.CustomerEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
}