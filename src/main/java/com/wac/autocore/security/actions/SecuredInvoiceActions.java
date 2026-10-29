package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.service.InvoiceService;

@Component 
public class SecuredInvoiceActions {

    private final InvoiceService invoiceService;

    public SecuredInvoiceActions(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    //Alla inloggade användare får visa fakturor
    @PreAuthorize ("isAuthenticated()")
    public List<Invoice> findAll(){
        return invoiceService.findAll();
    }

    //Hämtar fakturor till JavaFX
    @PreAuthorize ("isAuthenticated()")
    public List<Invoice> getLoadedInvoices(){
        return invoiceService.getLoadedInvoices();
    }

    //Endast ADMIN får förhandsgranska en ny faktura 
    @PreAuthorize ("hasRole('ADMIN')")
    public Invoice previewInvoice(
        int workerId,
        String discountCode,
        String extraDescription,
        double extraCost
    ){
        return invoiceService.previewInvoice(workerId, discountCode, extraDescription, extraCost);
    }

    //Endast ADMIN får skapa en faktura
    @PreAuthorize ("hasRole('ADMIN')")
    public Invoice creatInvoice(
        int workerId,
        String discountCode,
        String extraDescription,
        double extraCost
    ){
        return invoiceService.createInvoice(workerId, discountCode, extraDescription, extraCost);
    }


    //Endast ADMIN får spara fakturor
    @PreAuthorize ("hasRole('ADMIN')")
    public void save(Invoice invoice){
        invoiceService.save(invoice);
    }

}
