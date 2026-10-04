package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.ServiceItem;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class ServiceItemRepository {

    public void save(ServiceItem serviceItem) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(serviceItem);
                transaction.commit();

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    public List<ServiceItem> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<ServiceItem> serviceItems =
                        session.createQuery(
                                "from ServiceItem order by id",
                                ServiceItem.class
                        ).getResultList();

                transaction.commit();
                return serviceItems;

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    public List<ServiceItem> findAllServiceItems() {
        return findAll();
    }
}