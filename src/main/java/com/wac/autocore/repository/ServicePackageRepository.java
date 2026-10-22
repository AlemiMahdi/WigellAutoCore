package com.wac.autocore.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.wac.autocore.model.ServicePackage;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Repository
public class ServicePackageRepository implements IServicePackageRepository{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ServicePackage save( ServicePackage servicePackage) {
       entityManager.merge(servicePackage);
        return servicePackage;
    }

    @Override
    public List<ServicePackage> findAll() {
       return entityManager.createQuery("FROM ServicePackage", ServicePackage.class)
                    .getResultList();
    }
}
