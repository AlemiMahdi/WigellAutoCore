package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.VehicleEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// Hanterar lagring och hämtning av fordon i databasen.
public class VehicleRepository {

    public void save(VehicleEntity vehicle) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(vehicle);
                transaction.commit();
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public List<VehicleEntity> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<VehicleEntity> vehicles = session.createQuery(
                        "from VehicleEntity order by id",
                        VehicleEntity.class
                ).getResultList();

                transaction.commit();
                return vehicles;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }
}