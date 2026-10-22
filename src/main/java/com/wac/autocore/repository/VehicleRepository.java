package com.wac.autocore.repository;

import com.wac.autocore.model.Vehicle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Hanterar lagring och hämtning av fordon i databasen.
@Repository
@Transactional
public class VehicleRepository implements IVehicleRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Vehicle save(Vehicle vehicle) {
    entityManager.persist(vehicle);
    return vehicle;
    }

    @Override
    public List<Vehicle> findAll() {
                return entityManager.createQuery(
                        "from Vehicle order by id",
                        Vehicle.class
                ).getResultList();
    }

    @Override
    public Vehicle findById(int id) {
        return entityManager.find(Vehicle.class, id);
    }
}