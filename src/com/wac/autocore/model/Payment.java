package com.wac.autocore.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private int id;

    @Column(name = "invoice_id")
    private int invoiceId;

    @Column(name = "amount")
    private double amount;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "successful")
    private boolean successful;

    // Hibernate behöver en tom konstruktor.
    public Payment() {
    }

    public Payment(
            int id,
            int invoiceId,
            double amount,
            String paymentType
    ) {
        this.id = id;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.paymentDate = LocalDateTime.now();
        this.successful = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Override
    public String toString() {
        return id +
                " - Invoice ID: " + invoiceId +
                " | Amount: " + amount + " SEK" +
                " | Payment type: " + paymentType +
                " | Date: " + paymentDate +
                " | Successful: " +
                (successful ? "Yes" : "No");
    }
}