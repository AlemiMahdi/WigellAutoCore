package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class MechanicRepository implements IMechanicRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Mechanic save(Mechanic mechanic) {
        entityManager.persist(mechanic);
        return mechanic;
    }

    @Override
    public List<Mechanic> findAll() {
        return entityManager.createQuery(
                "from Mechanic order by id",
                Mechanic.class
        ).getResultList();
    }
}