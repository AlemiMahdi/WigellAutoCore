package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.VehicleEntity;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.wac.autocore.model.Vehicle;
import java.util.ArrayList;

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

    // Omvandlar originalets fordon till en entity och sparar det.
    public void save(Vehicle vehicle) {
        VehicleEntity entity = new VehicleEntity();

        entity.setId(vehicle.getId());
        entity.setRegistrationNumber(vehicle.getRegistrationNumber());
        entity.setBrand(vehicle.getBrand());
        entity.setModel(vehicle.getModel());
        entity.setYear(vehicle.getYear());
        entity.setCustomerId(vehicle.getCustomerId());

        save(entity);
    }

    // Hämtar databasens fordon som originalets Vehicle-objekt.
    public List<Vehicle> findAllVehicles() {
        List<Vehicle> vehicles = new ArrayList<>();

        for (VehicleEntity entity : findAll()) {
            Vehicle vehicle = new Vehicle(
                    entity.getId(),
                    entity.getRegistrationNumber(),
                    entity.getBrand(),
                    entity.getModel(),
                    entity.getYear(),
                    entity.getCustomerId()
            );

            vehicles.add(vehicle);
        }

        return vehicles;
    }
}