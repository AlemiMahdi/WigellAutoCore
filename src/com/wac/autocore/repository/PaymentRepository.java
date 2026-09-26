package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.PaymentEntity;
import com.wac.autocore.model.Payment;
import com.wac.autocore.entity.InvoiceEntity;
import com.wac.autocore.model.Invoice;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {

    public void save(PaymentEntity payment) {

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

    // Omvandlar originalets Payment till PaymentEntity och sparar den.
    public void save(Payment payment) {

        PaymentEntity entity = new PaymentEntity();

        entity.setId(payment.getId());
        entity.setInvoiceId(payment.getInvoiceId());
        entity.setAmount(payment.getAmount());
        entity.setPaymentType(payment.getPaymentType());
        entity.setPaymentDate(payment.getPaymentDate());
        entity.setSuccessful(payment.isSuccessful());

        save(entity);
    }

    public List<PaymentEntity> findAll() {

        try (Session session =
                     HibernateUtil.getSessionFactory().openSession()) {

            Transaction transaction = session.beginTransaction();

            try {

                List<PaymentEntity> payments =
                        session.createQuery(
                                "from PaymentEntity order by id",
                                PaymentEntity.class
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

    // Hämtar databasens entities som originalets Payment-objekt.
    public List<Payment> findAllPayments() {

        List<Payment> payments = new ArrayList<>();

        for (PaymentEntity entity : findAll()) {

            Payment payment = new Payment(
                    entity.getId(),
                    entity.getInvoiceId(),
                    entity.getAmount(),
                    entity.getPaymentType()
            );

            // Konstruktor sätter tiden till "nu",
            // därför återställer vi den sparade tiden från databasen.
            payment.setPaymentDate(entity.getPaymentDate());

            payment.setSuccessful(
                    entity.isSuccessful()
            );

            payments.add(payment);
        }

        return payments;
    }

    public void savePaymentAndInvoice(Payment payment, Invoice invoice) {

    try (Session session =
                 HibernateUtil.getSessionFactory().openSession()) {

        Transaction transaction = session.beginTransaction();

        try {

            PaymentEntity paymentEntity = new PaymentEntity();

            paymentEntity.setId(payment.getId());
            paymentEntity.setInvoiceId(payment.getInvoiceId());
            paymentEntity.setAmount(payment.getAmount());
            paymentEntity.setPaymentType(payment.getPaymentType());
            paymentEntity.setPaymentDate(payment.getPaymentDate());
            paymentEntity.setSuccessful(payment.isSuccessful());


            InvoiceEntity invoiceEntity = new InvoiceEntity();

            invoiceEntity.setId(invoice.getId());
            invoiceEntity.setWorkOrderId(invoice.getWorkOrderId());
            invoiceEntity.setInvoiceDate(invoice.getInvoiceDate());
            invoiceEntity.setAmount(invoice.getAmount());
            invoiceEntity.setDiscount(invoice.getDiscount());
            invoiceEntity.setTotalAmount(invoice.getTotalAmount());
            invoiceEntity.setPaid(invoice.isPaid());


            session.saveOrUpdate(paymentEntity);
            session.saveOrUpdate(invoiceEntity);

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