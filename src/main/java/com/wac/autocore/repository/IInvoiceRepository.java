package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import java.util.List;

public interface IInvoiceRepository {

    Invoice save(Invoice invoice);

    List<Invoice> findAll();

    List<Invoice> findAllInvoices();

}
