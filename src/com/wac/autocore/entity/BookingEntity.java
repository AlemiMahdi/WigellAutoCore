package com.wac.autocore.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.ManyToMany;

import com.wac.autocore.model.ServiceItem;


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

    @Column( name = "start_time")
    private LocalTime startTime;

    @Column (name = "duration_minutes")
    private Integer durationMinutes;

    @ManyToMany 
    @JoinTable (
        name = "booking_services",
        joinColumns = @JoinColumn (name = "booking_id"),
        inverseJoinColumns = @JoinColumn (name = "service_id")
    )
    private List<ServiceItem> services = new ArrayList<>(); 

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

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime (LocalTime startTime) { this.startTime = startTime; }

    public Integer getDurationMinutes () { return durationMinutes; }
    public void setDurationMinutes (Integer duration_minutes ) { this.durationMinutes = duration_minutes; }

    public List<ServiceItem> getServices () { return  services;}
    public void setServices ( List<ServiceItem> services) { this.services = services; }



}
