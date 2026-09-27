package com.wac.autocore.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class BookingEntity {

    @Id
    private int id;

    @Column (name = "vehicle_id")
    private int vehicleId;

    @Column(name = "booking_date")
    private LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "mechanic_id")
    private Integer mechanicId;

    public BookingEntity() {}

    public int getId(){ return id; }

    public void setId(int id){ this.id = id; }

    public int getVehicleId(){ return vehicleId; }

    public void setVehicleId(int vehicleId){ this.vehicleId = vehicleId; }

    public LocalDate getDate(){ return date; }

    public void setDate(LocalDate date) { this.date = date; }

    public String getDescription(){ return description; }

    public void setDescription(String description){ this.description = description; }

    public String getStatus(){ return status; }

    public void setStatus (String status){ this.status = status;}

    public Integer getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(Integer mechanicId) {
        this.mechanicId = mechanicId;
    }



}
