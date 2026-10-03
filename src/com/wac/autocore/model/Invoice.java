package com.wac.autocore.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

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

    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    @JoinColumn(name = "invoice_id")
    private List<InvoiceLine> lines = new ArrayList<>();

    // Hibernate behöver en tom konstruktor.
    public Invoice() {
    }

    public Invoice(
            int id,
            int workOrderId,
            LocalDate invoiceDate,
            double amount
    ) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.discount = 0.0;
        this.totalAmount = amount;
        this.paid = false;
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
        calculateTotalAmount();
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotalAmount();
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void addLine(InvoiceLine invoiceLine) {
        lines.add(invoiceLine);
        calculateAmountFromLines();
    }

    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    private void calculateTotalAmount() {
        this.totalAmount = amount - discount;
    }

    private void calculateAmountFromLines() {

        double sum = 0.0;

        for (InvoiceLine line : lines) {
            sum += line.getFinalPrice();
        }

        amount = sum;
        calculateTotalAmount();
    }

    @Override
    public String toString() {
        return id +
                " - Work order ID: " + workOrderId +
                " | Date: " + invoiceDate +
                " | Amount: " + amount + " SEK" +
                " | Discount: " + discount + " SEK" +
                " | Total: " + totalAmount + " SEK" +
                " | Paid: " + (paid ? "Yes" : "No");
    }
}