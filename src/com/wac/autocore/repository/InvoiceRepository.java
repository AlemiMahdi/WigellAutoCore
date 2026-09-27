package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.InvoiceEntity;
import com.wac.autocore.model.Invoice;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class InvoiceRepository {

    public void save(InvoiceEntity invoice) {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {
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

    // Omvandlar originalets Invoice till InvoiceEntity och sparar den.
    public void save(Invoice invoice) {

        InvoiceEntity entity = new InvoiceEntity();

        entity.setId(invoice.getId());
        entity.setWorkOrderId(invoice.getWorkOrderId());
        entity.setInvoiceDate(invoice.getInvoiceDate());
        entity.setAmount(invoice.getAmount());
        entity.setDiscount(invoice.getDiscount());
        entity.setTotalAmount(invoice.getTotalAmount());
        entity.setPaid(invoice.isPaid());

        save(entity);
    }

    public List<InvoiceEntity> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<InvoiceEntity> invoices =
                        session.createQuery(
                                "from InvoiceEntity order by id",
                                InvoiceEntity.class
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

    // Hämtar databasens entities som originalets Invoice-objekt.
    public List<Invoice> findAllInvoices() {

        List<Invoice> invoices = new ArrayList<>();

        for (InvoiceEntity entity : findAll()) {

            Invoice invoice = new Invoice(
                    entity.getId(),
                    entity.getWorkOrderId(),
                    entity.getInvoiceDate(),
                    entity.getAmount()
            );

            invoice.setDiscount(entity.getDiscount());
            invoice.setPaid(entity.isPaid());

            invoices.add(invoice);
        }

        return invoices;
    }
}