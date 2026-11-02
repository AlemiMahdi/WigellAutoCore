package com.wac.autocore.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "service_item_id")
    private int serviceItemId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "price")
    private double price;

    @Column(name = "discount")
    private double discount;

    @Column(name = "final_price")
    private double finalPrice;

    // Hibernate behöver en tom konstruktor.
    public InvoiceLine() {
    }

    public InvoiceLine(
            int serviceItemId,
            String serviceName,
            double price
    ) {
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.price = price;
        this.discount = 0.0;
        calculateTotalFinalPrice();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getPrice() {
        return price;
    }

    public double getDiscount() {
        return discount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    private void calculateTotalFinalPrice() {
        finalPrice = price - discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
        calculateTotalFinalPrice();
    }
}