package com.wac.autocore.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    private int id;

    @Column(name = "vehicle_id")
    private int vehicleId;

    @Column(name = "booking_date")
    private LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @ManyToMany
    @JoinTable(
            name = "booking_services",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ServiceItem> services = new ArrayList<>();

    @Column(name = "mechanic_id")
    private Integer mechanicId;

    // Historiska priser för tjänsterna i bokningen.
    // Sparas som snapshot så framtida prisändringar inte påverkar bokningen.
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "booking_id")
    private List<InvoiceLine> frozenPrice = new ArrayList<>();

    // Hibernate behöver en tom konstruktor.
    public Booking() {
    }

    public Booking(int id, int vehicleId, LocalDate date, String description) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.description = description;
        this.status = "BOOKED";
    }

    public Booking(
            int id,
            int vehicleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            String description
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.status = "BOOKED";
    }

    public Booking(
            int id,
            int vehicleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            String description,
            List<InvoiceLine> frozenPrice
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.date = date;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.description = description;
        this.status = "BOOKED";
        this.frozenPrice = frozenPrice != null
                ? frozenPrice
                : new ArrayList<>();
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

    public List<ServiceItem> getServices() {
        return services;
    }

    public void setServices(List<ServiceItem> services) {
        this.services = services;
    }

    public Integer getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(Integer mechanicId) {
        this.mechanicId = mechanicId;
    }

    public List<InvoiceLine> getFrozenPrice() {
        return frozenPrice;
    }

    public void setFrozenPrice(List<InvoiceLine> frozenPrice) {
        this.frozenPrice = frozenPrice != null
                ? frozenPrice
                : new ArrayList<>();
    }

    public void addFrozenPrice(InvoiceLine line) {
        if (line != null) {
            frozenPrice.add(line);
        }
    }

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