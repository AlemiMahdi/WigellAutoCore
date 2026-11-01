package com.wac.autocore.security.actions;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.service.PaymentService;

@Component 
public class SecuredPaymentActions {
    
    private final PaymentService paymentService;

    public SecuredPaymentActions(PaymentService paymentService){
        this.paymentService = paymentService;
    }

    //All inloggade användare får se registrerade betalningar
    @PreAuthorize ("isAuthenticated()")
    public List<Payment> findAll(){
        return List.copyOf(paymentService.findAll());
    }

    //Alla inloggade användare får se obetalda fakturor
    @PreAuthorize ("hasRole('ADMIN')")
    public List<Invoice> findUnpaidInvoices(){
        return List.copyOf(PaymentService.findUnpaidInvoices());
    }

    //Endast ADMIN får regisrera betalningar
    @PreAuthorize ("hasRole('ADMIN')")
    public void processPayment(
        Invoice selectedInvoice,
        String paymentType
    ){
        paymentService.processPayment(selectedInvoice, paymentType);
    }

}
