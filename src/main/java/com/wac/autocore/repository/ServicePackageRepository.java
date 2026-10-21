package com.wac.autocore.repository;

import java.util.List;

import org.hibernate.*;
import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.ServicePackage;
import org.hibernate.event.spi.PersistContext;

public class ServicePackageRepository implements IServicePackageRepository{

    @PersistContext

    @Override
    public ServicePackage save( ServicePackage servicePackage) {
        try ( Session session =
                HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                ServicePackage save = (ServicePackage) session.merge(servicePackage);
                transaction.commit();
                return save;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }

        }
    }

    @Override
    public List<ServicePackage> findAll() {
       entityManager
                    .createQuery("FROM ServicePackage", ServicePackage.class)
                    .getResultList();
        }
    }
}
