package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.ServiceItem;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class ServiceItemRepository implements IServiceItemRepository {

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

    public ServiceItem findById(int id) {
        // Try-with-resources öppnar och stänger sessionen automatiskt
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Hämtar raden live från tabellen service_items via dess primärnyckel
            return session.get(ServiceItem.class, id);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta tjänst från databasen med ID: " + id, e);
        }
    }

    public List<ServiceItem> findAllByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                List<ServiceItem> serviceItems = session.createQuery(
                                "from ServiceItem where id in :ids order by id",
                                ServiceItem.class
                        )
                        .setParameter("ids", ids)
                        .getResultList();

                transaction.commit();return serviceItems;

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
}