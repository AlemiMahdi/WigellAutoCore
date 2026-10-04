package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Vehicle;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

// Hanterar lagring och hämtning av fordon i databasen.
public class VehicleRepository {

    public void save(Vehicle vehicle) {
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

    public List<Vehicle> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<Vehicle> vehicles = session.createQuery(
                        "from Vehicle order by id",
                        Vehicle.class
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

    public List<Vehicle> findAllVehicles() {
        return findAll();
    }
}