package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.WorkOrder;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class WorkOrderRepository {

    public void save(WorkOrder workOrder) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                session.saveOrUpdate(workOrder);

                transaction.commit();

            } catch (RuntimeException e) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        }
    }

    public List<WorkOrder> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<WorkOrder> workOrders =
                        session.createQuery(
                                "from WorkOrder order by id",
                                WorkOrder.class
                        ).getResultList();

                transaction.commit();

                return workOrders;

            } catch (RuntimeException e) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw e;
            }
        }
    }

    public List<WorkOrder> findAllWorkOrders() {
        return findAll();
    }
}