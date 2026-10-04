package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Mechanic;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class MechanicRepository {

    public void save(Mechanic mechanic) {
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

    public List<Mechanic> findAll() {
        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<Mechanic> mechanics =
                        session.createQuery(
                                "from Mechanic order by id",
                                Mechanic.class
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

    public List<Mechanic> findAllMechanics() {
        return findAll();
    }
}