package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.model.Invoice;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;


public class InvoiceRepository implements IInvoiceRepository {

    @Override
    public Invoice save(Invoice invoice) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                Invoice savedInvoice = (Invoice) session.merge(invoice);

                transaction.commit();

                return savedInvoice;

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    @Override
    public List<Invoice> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<Invoice> invoices =
                        session.createQuery(
                                "select distinct i from Invoice i " +
                                        "left join fetch i.lines " +
                                        "order by i.id",
                                Invoice.class
                        ).getResultList();

                transaction.commit();

                return invoices;

            } catch (RuntimeException exception) {

                if (transaction.isActive()) {
                    transaction.rollback();
                }

                throw exception;
            }
        }
    }

    @Override
    public List<Invoice> findAllInvoices() {
        return findAll();
    }
}