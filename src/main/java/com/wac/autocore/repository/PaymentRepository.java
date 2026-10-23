package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
@Transactional
public class PaymentRepository implements IPaymentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Payment save(Payment payment) {
        entityManager.persist(payment);
        return payment;
    }

    public List<Payment> findAll() {

     return entityManager.createQuery(
                                "from Payment order by id",
                                Payment.class
                        ).getResultList();
    }

    public void savePaymentAndInvoice(
            Payment payment,
            Invoice invoice
    ) {
        entityManager.persist(payment);
        entityManager.merge(invoice);
    }
}