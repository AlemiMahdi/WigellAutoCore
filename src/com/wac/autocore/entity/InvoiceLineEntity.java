package com.wac.autocore.entity;

import javax.persistence.*;

@Entity
@Table (name = "invoice_lines")
public class InvoiceLineEntity {

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

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private BookingEntity booking;

    public InvoiceLineEntity() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public void setServiceItemId(int serviceItemId) {
        this.serviceItemId = serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public BookingEntity getBooking() { return booking; }

    public void setBooking(BookingEntity booking) { this.booking = booking; }
}
