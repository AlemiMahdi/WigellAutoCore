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

    public ServiceItem save(ServiceItem serviceItem) {
        entityManager.persist(serviceItem);
        return serviceItem;
    }

    public void update(ServiceItem serviceItem) {
        entityManager.merge(serviceItem);
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

    public List<ServiceItem> findAllByIds(List<Integer> ids){
        return entityManager.createQuery("SELECT s FROM ServiceItem s where s.id in :ids", ServiceItem.class)
                .setParameter("ids", ids).getResultList();
    }
}