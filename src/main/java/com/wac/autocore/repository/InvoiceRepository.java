package com.wac.autocore.repository;

import com.wac.autocore.DTO.InvoiceableOrder;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional
public class InvoiceRepository implements IInvoiceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Invoice save(Invoice invoice) {
        entityManager.persist(invoice);
        return invoice;
    }

    @Override
    public List<Invoice> findAll() {

            return entityManager.createQuery(
                                "select distinct i from Invoice i " +
                                        "left join fetch i.lines " +
                                        "order by i.id",
                                Invoice.class
                        ).getResultList();

    }

    @Override
    @SuppressWarnings("unchecked")
    public List<InvoiceableOrder> findInvoiceableWorkOrdersWithVehicle(){
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT wo.id, wo.booking_id, wo.mechanic_id, wo.original_work_order_id, " +
                        "wo.status, wo.work_order_type, wo.estimated_price, wo.customer_instructions, wo.draft_comments, " +
                        "v.brand, v.model " +
                        "FROM work_orders wo " +
                        "JOIN bookings b ON wo.booking_id = b.id " +
                        "JOIN vehicles v ON b.vehicle_id = v.id " +
                        "WHERE wo.status = 'COMPLETED' " +
                        "AND wo.id NOT IN (SELECT i.work_order_id FROM invoices i WHERE i.work_order_id IS NOT NULL)"
        ).unwrap(NativeQuery.class)
                .addEntity("wo", WorkOrder.class)
                .addScalar("brand", StandardBasicTypes.STRING)
                .addScalar("model", StandardBasicTypes.STRING)
                .getResultList();
        //mappa om datan
        List<InvoiceableOrder> dtoOrders = new ArrayList<>();
        for (Object[] row : rows) {
            WorkOrder wo = (WorkOrder) row[0];
            String brand = (String) row[1];
            String model = (String) row[2];
            dtoOrders.add(new InvoiceableOrder(wo, brand, model));
        }
        return dtoOrders;
    }
}