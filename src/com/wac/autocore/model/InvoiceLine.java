package com.wac.autocore.model;

public class InvoiceLine {

    private int id;
    private int serviceItemId;
    private String serviceName;
    private double price;
    private double discount;
    private double finalPrice;

    public InvoiceLine(int serviceItemId,
                       String serviceName, double price

    ) {

        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.price = price;
        this.discount = 0.0;
        this.calculateTotalFinalPrice();
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
