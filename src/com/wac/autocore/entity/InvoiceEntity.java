package com.wac.autocore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

// Databasrepresentation av en faktura.
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private int id;

    @Column(name = "work_order_id")
    private int workOrderId;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "amount")
    private double amount;

    @Column(name = "discount")
    private double discount;

    @Column(name = "total_amount")
    private double totalAmount;

    @Column(name = "paid")
    private boolean paid;

    // Hibernate behöver en tom konstruktor.
    public InvoiceEntity() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(int workOrderId) {
        this.workOrderId = workOrderId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}