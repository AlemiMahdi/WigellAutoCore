package com.wac.autocore.service;

import com.wac.autocore.model.InvoiceLine;
import com.wac.autocore.model.ServiceItem;

import java.util.ArrayList;
import java.util.List;

public class InvoiceService {

    private static InvoiceService instance;

    private InvoiceService() {}

    public static synchronized InvoiceService getInstance() {
        if (instance == null) {
            instance = new InvoiceService();
        }
        return instance;
    }

    public List<InvoiceLine> createFrozenPrices(List<ServiceItem> serviceItems) {
        List<InvoiceLine> frozenPrices = new ArrayList<>();
        for (ServiceItem service : serviceItems) {
            InvoiceLine frozenLine = new InvoiceLine(
                    service.getId(),
                    service.getName(),
                    service.getPrice() // Sparar priset inför faktureringen
            );
            frozenPrices.add(frozenLine);
        }
        return frozenPrices;
    }
}
