package com.wac.autocore.DTO;

import com.wac.autocore.model.ServiceItem;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WorkOrderDto {
    private Integer bookingId;
    private Integer customerId;
    private Integer vehicleId;
    private String description;
    private String status;
    private List<Integer> services;
    private Integer mechanicId;
    private double price;

    public WorkOrderDto(){}

    //drop_in arbetsorder
    public WorkOrderDto(int bookingId, int customerId, Integer vehicleId, String description, String status, List<Integer> services, Integer mechanicId, double price) {
        this.bookingId = bookingId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.description = description;
        this.status = status;
        this.services = services;
        this.mechanicId = mechanicId;
        this.price = price;
    }
    //planned arbetsorder
    public WorkOrderDto(int bookingId, int mechanicId){
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public List<Integer> getServices() {
        return services;
    }

    public Integer getMechanicId() {
        return mechanicId;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public double getPrice(){
        return price;
    }

    public void setBookingId(Integer bookingId) {
        this.bookingId = bookingId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setServices(List<Integer> services) {
        this.services = services;
    }

    public void setMechanicId(Integer mechanicId) {
        this.mechanicId = mechanicId;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
