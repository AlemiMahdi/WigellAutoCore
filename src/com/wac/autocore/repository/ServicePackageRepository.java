package com.wac.autocore.repository;

import java.util.List;

import org.hibernate.*;
import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.ServicePackage;

public class ServicePackageRepository implements IServicePackageRepository{

    @Override
    public void save( ServicePackage servicePackage) {
        try ( Session session =
                HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(servicePackage);
                transaction.commit();
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
        try( Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session
                    .createQuery("FROM ServicePackage", ServicePackage.class)
                    .getResultList();
        }
    }
}
