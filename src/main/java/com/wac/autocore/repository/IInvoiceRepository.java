package com.wac.autocore.repository;

import com.wac.autocore.model.Invoice;
import java.util.List;

public interface IInvoiceRepository {

    void save(Invoice invoice);

    void findAll();
}
