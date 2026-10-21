package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class MechanicRepository implements IMechanicRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void save(Mechanic mechanic) {
        entityManager.persist(mechanic);
    }

    @Override
    public void findAll() {
        entityManager.createQuery(
                "from Mechanic order by id",
                Mechanic.class
        ).getResultList();
    }
}