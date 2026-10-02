package com.wac.autocore.repository;

import com.wac.autocore.data.HibernateUtil;
import com.wac.autocore.entity.InvoiceEntity;
import com.wac.autocore.entity.InvoiceLineEntity;
import com.wac.autocore.model.Invoice;

import com.wac.autocore.model.InvoiceLine;
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

        //    Kopiera varje fakturarad till en InvoiceLineEntity.
        //    Loop eftersom en faktura kan ha 0, 1 eller många rader.
        for (InvoiceLine invoiceLine : invoice.getLines()) {
            InvoiceLineEntity lineEntity = new InvoiceLineEntity();

            lineEntity.setId(invoiceLine.getId());
            lineEntity.setServiceItemId(invoiceLine.getServiceItemId());
            lineEntity.setServiceName(invoiceLine.getServiceName());
            lineEntity.setPrice(invoiceLine.getPrice());
            lineEntity.setDiscount(invoiceLine.getDiscount());
            lineEntity.setFinalPrice(invoiceLine.getFinalPrice());

            // Raden måste ligga i fakturans lista, annars sparas den inte (cascade).
            entity.getLines().add(lineEntity);
        }

        save(entity);

        //    Skicka tillbaka id:n som databasen gav raderna till modellen,
        //    så att raderna uppdateras (inte dubbleras) nästa gång fakturan sparas.
        for (int i = 0; i < entity.getLines().size(); i++) {
            int generatedId = entity.getLines().get(i).getId();
            invoice.getLines().get(i).setId(generatedId);
        }
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

            // Hämta fakturans rader från databasen och lägg dem på modellen.
            // Gamla fakturor har inga rader → loopen körs inte och amount från databasen behålls.
            for (InvoiceLineEntity lineEntity : entity.getLines()) {
                InvoiceLine line = new InvoiceLine(lineEntity.getServiceItemId(),lineEntity.getServiceName(),lineEntity.getPrice());

                // Id behövs så att raden uppdateras (inte dubbleras) om fakturan sparas igen.
                line.setId(lineEntity.getId());
                line.setDiscount(lineEntity.getDiscount());

                // addLine lägger till raden och räknar om fakturans summa.
                invoice.addLine(line);
            }

            invoice.setDiscount(entity.getDiscount());
            invoice.setPaid(entity.isPaid());

            invoices.add(invoice);
        }

        return invoices;
    }
}