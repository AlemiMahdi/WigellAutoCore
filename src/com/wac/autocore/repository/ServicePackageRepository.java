package com.wac.autocore.repository;

import java.util.List;

import org.hibernate.*;
import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.ServicePackage;

public class ServicePackageRepository {
    
    public void save( ServicePackage servicePackage) {
        Transaction transaction = null;

        try( Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.saveOrUpdate(servicePackage);
            transaction.commit();

        } catch (RuntimeException exception) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw exception;
        }
    }

    public List<ServicePackage> findAll() {
        try( Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session
                    .createQuery("FROM ServicePackage", ServicePackage.class)
                    .getResultList();
        }
    }
}
