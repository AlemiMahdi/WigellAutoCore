package com.wac.autocore.repository;

import com.wac.autocore.model.ServiceItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ServiceItemRepository implements IServiceItemRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ServiceItem save(ServiceItem serviceItem) {

        // Ny tjänst – MySQL genererar ett nytt ID
        if (serviceItem.getId() == 0) {
            entityManager.persist(serviceItem);
            return serviceItem;
        }

        // Befintlig tjänst – uppdatera den i databasen
        return entityManager.merge(serviceItem);
    }

    public List<ServiceItem> findAll() {
        return entityManager.createQuery(
                "from ServiceItem order by id",
                ServiceItem.class
        ).getResultList();
    }

    public ServiceItem findById(int id) {
        return entityManager.find(ServiceItem.class, id);
    }
}