package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.ServiceItemEntity;
import com.wac.autocore.model.ServiceItem;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class ServiceItemRepository {

    public void save(ServiceItemEntity serviceItem) {
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

    // Omvandlar originalets ServiceItem till Entity och sparar den.
    public void save(ServiceItem serviceItem) {

        ServiceItemEntity entity = new ServiceItemEntity();

        entity.setId(serviceItem.getId());
        entity.setName(serviceItem.getName());
        entity.setDescription(serviceItem.getDescription());
        entity.setPrice(serviceItem.getPrice());
        entity.setEstimatedMinutes(serviceItem.getEstimatedMinutes());

        save(entity);
    }

    public List<ServiceItemEntity> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<ServiceItemEntity> serviceItems =
                        session.createQuery(
                                "from ServiceItemEntity order by id",
                                ServiceItemEntity.class
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

    // Hämtar databasens entities som originalets ServiceItem-objekt.
    public List<ServiceItem> findAllServiceItems() {

        List<ServiceItem> serviceItems = new ArrayList<>();

        for (ServiceItemEntity entity : findAll()) {

            ServiceItem serviceItem = new ServiceItem(
                    entity.getId(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getPrice(),
                    entity.getEstimatedMinutes()
            );

            serviceItems.add(serviceItem);
        }

        return serviceItems;
    }
}