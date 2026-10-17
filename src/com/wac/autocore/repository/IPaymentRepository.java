package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;

import java.util.List;

public interface IPaymentRepository {
    void save(Payment payment);
    List<Payment> findAll();
    void savePaymentAndInvoice(Payment payment, Invoice invoice);
}
