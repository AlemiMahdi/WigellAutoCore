package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;

import java.util.List;

public interface IPaymentRepository {
    void save(Payment payment);
    List<Payment> findAll();
    List<Payment> findAllPayments();
    void savePaymentAndInvoice(Payment payment, Invoice invoice);
}
