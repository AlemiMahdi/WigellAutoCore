package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}