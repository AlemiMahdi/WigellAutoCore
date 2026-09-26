package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.WorkOrderEntity;
import com.wac.autocore.model.WorkOrder;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class WorkOrderRepository {

    public void save(WorkOrderEntity workOrderEntity) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            try {
                session.saveOrUpdate(workOrderEntity);
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    public void save(WorkOrder workOrder) {

        WorkOrderEntity entity = new WorkOrderEntity();

        entity.setId(workOrder.getId());
        entity.setBookingId(workOrder.getBookingId());
        entity.setMechanicId(workOrder.getMechanicId());
        entity.setServiceItemIds(new ArrayList<>(workOrder.getServiceItemIds()));
        entity.setStatus(workOrder.getStatus());

        save(entity);


    }

    public List<WorkOrderEntity> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
                List<WorkOrderEntity> workOrders = session.createQuery("from WorkOrderEntity order by id", WorkOrderEntity.class).getResultList();
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

        List<WorkOrder> workOrders = new ArrayList<>();

        for (WorkOrderEntity entity : findAll()) {

            WorkOrder workOrder = new WorkOrder(entity.getId(), entity.getBookingId(), entity.getMechanicId()


            );

            workOrder.setServiceItemIds(new ArrayList<>(entity.getServiceItemIds()));
            workOrder.setStatus(entity.getStatus());


            workOrders.add(workOrder);
        }
        return workOrders;
    }

}
