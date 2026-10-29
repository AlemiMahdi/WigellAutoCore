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
        entityManager.persist(serviceItem);
        return serviceItem;
    }

    @Override
    public void update(ServiceItem serviceItem) {
        entityManager.merge(serviceItem);
    }

    @Override
    public List<ServiceItem> findAll() {
        return entityManager.createQuery(
                "from ServiceItem order by id",
                ServiceItem.class
        ).getResultList();
    }

    @Override
    public ServiceItem findById(int id) {
        return entityManager.find(ServiceItem.class, id);
    }

    @Override
    public List<ServiceItem> findAllByIds(List<Integer> ids){
        return entityManager.createQuery("SELECT s FROM ServiceItem s where s.id in :ids", ServiceItem.class)
                .setParameter("ids", ids).getResultList();
    }
}