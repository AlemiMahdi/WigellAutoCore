package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PaymentRepository {

    public void save(Payment payment) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                session.saveOrUpdate(payment);

                transaction.commit();

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    public List<Payment> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<Payment> payments =
                        session.createQuery(
                                "from Payment order by id",
                                Payment.class
                        ).getResultList();

                transaction.commit();

                return payments;

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    public List<Payment> findAllPayments() {
        return findAll();
    }

    public void savePaymentAndInvoice(
            Payment payment,
            Invoice invoice
    ) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                session.saveOrUpdate(payment);
                session.saveOrUpdate(invoice);

                transaction.commit();

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }
}