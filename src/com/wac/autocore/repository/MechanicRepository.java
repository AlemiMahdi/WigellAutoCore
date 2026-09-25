package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.MechanicEntity;
import com.wac.autocore.model.Mechanic;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class MechanicRepository {

    public void save(MechanicEntity mechanic) {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(mechanic);
                transaction.commit();
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    // Omvandlar originalets Mechanic till Entity och sparar den.
    public void save(Mechanic mechanic) {

        MechanicEntity entity = new MechanicEntity();

        entity.setId(mechanic.getId());
        entity.setName(mechanic.getName());
        entity.setPhone(mechanic.getPhone());
        entity.setSpecialization(mechanic.getSpecialization());
        entity.setAvailable(mechanic.isAvailable());

        save(entity);
    }

    public List<MechanicEntity> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<MechanicEntity> mechanics =
                        session.createQuery(
                                "from MechanicEntity order by id",
                                MechanicEntity.class
                        ).getResultList();

                transaction.commit();
                return mechanics;

            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    // Hämtar databasens entities som originalets Mechanic-objekt.
    public List<Mechanic> findAllMechanics() {

        List<Mechanic> mechanics = new ArrayList<>();

        for (MechanicEntity entity : findAll()) {

            Mechanic mechanic = new Mechanic(
                    entity.getId(),
                    entity.getName(),
                    entity.getPhone(),
                    entity.getSpecialization()
            );

            // Konstruktor sätter available = true,
            // så vi återställer det sparade värdet här.
            mechanic.setAvailable(entity.isAvailable());

            mechanics.add(mechanic);
        }

        return mechanics;
    }
}