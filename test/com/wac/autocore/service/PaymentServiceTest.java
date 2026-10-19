package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.exception.BusinessRuleException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.repository.IPaymentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    @Test
    void missingInvoiceIsValidationError() {
        FakePaymentRepository repository = new FakePaymentRepository();
        PaymentService service = new PaymentService(repository);

        ValidationException error = assertThrows(
                ValidationException.class,
                () -> service.processPayment(null, "CARD")
        );

        assertEquals("processPayment.selectInvoice", error.getMessageKey());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void paidInvoiceIsBusinessRuleError() {
        FakePaymentRepository repository = new FakePaymentRepository();
        PaymentService service = new PaymentService(repository);
        Invoice invoice = new Invoice(1, LocalDate.now(), 500.0);
        invoice.setPaid(true);

        BusinessRuleException error = assertThrows(
                BusinessRuleException.class,
                () -> service.processPayment(invoice, "CARD")
        );

        assertEquals("processPayment.alreadyPaid", error.getMessageKey());
        assertTrue(invoice.isPaid());
        assertEquals(0, repository.saveCalls);
    }

    @Test
    void storageFailureRestoresInvoiceAndPreservesPayments() {
        List<Payment> originalPayments =
                new ArrayList<>(Database.getPayments());

        try {
            FakePaymentRepository repository = new FakePaymentRepository();
            RuntimeException failure =
                    new IllegalStateException("Database unavailable");
            repository.failure = failure;

            PaymentService service = new PaymentService(repository);
            Invoice invoice = new Invoice(1, LocalDate.now(), 500.0);

            DataAccessException error = assertThrows(
                    DataAccessException.class,
                    () -> service.processPayment(invoice, "CARD")
            );

            assertEquals("processPayment.saveError", error.getMessageKey());
            assertSame(failure, error.getCause());
            assertFalse(invoice.isPaid());
            assertEquals(1, repository.saveCalls);
            assertEquals(originalPayments, Database.getPayments());
        } finally {
            Database.getPayments().clear();
            Database.getPayments().addAll(originalPayments);
        }
    }

    private static class FakePaymentRepository
            implements IPaymentRepository {

        private int saveCalls;
        private RuntimeException failure;

        @Override
        public void savePaymentAndInvoice(Payment payment, Invoice invoice) {
            saveCalls++;
            if (failure != null) {
                throw failure;
            }
        }

        @Override
        public void save(Payment payment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Payment> findAll() {
            return new ArrayList<>();
        }
    }
}