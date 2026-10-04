package com.wac.autocore.model;

import com.wac.autocore.entity.InvoiceLineEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    private int id;
    private int vehicleId;
    private LocalDate date;
    private String description;
    private String status;
    private LocalTime startTime;
    private int durationMinutes;
    private List<ServiceItem> services = new ArrayList();
    private List<InvoiceLine> frozenPrice = new ArrayList<>();

    public Booking(int id, int vehicleId, LocalDate date, String description) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
        this.frozenPrice = new ArrayList<>();
    }

    public Booking(int id, int vehicleId, LocalDate date, LocalTime startTime, int durationMinutes, String description, List<InvoiceLine> frozenPrice) {

        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.status = "BOOKED";
        this.frozenPrice = frozenPrice;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalTime getStartTime() {
    return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public List<ServiceItem> getServices () { return services; }
    public void setServices (List<ServiceItem> services) { this.services = services; }

    public void setFrozenPrice(List<InvoiceLine> frozenLines) {
        this.frozenPrice = frozenLines;
    }

    // En smidig hjälpmetod för att lägga till en enskild rad
    public void addFrozenPrice(List<InvoiceLineEntity> lines) {
        if (lines == null) return;

        for (InvoiceLineEntity entity : lines) {
            InvoiceLine domainLine = new InvoiceLine(
                    entity.getServiceItemId(),
                    entity.getServiceName(),
                    entity.getPrice()
            );
            this.frozenPrice.add(domainLine);
        }
        }
    public List<InvoiceLine> getFrozenPrice() { return frozenPrice; }

    @Override
    public String toString() {
        return id + " - Vehicle ID: " + vehicleId +
                " | Date: " + date +
                " | Start: " + startTime +
                " | Duration: " + durationMinutes + " min" +
                " | Description: " + description +
                " | Status: " + status;
    }
}