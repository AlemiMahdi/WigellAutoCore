package com.wac.autocore.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table (name ="work_orders")
public class WorkOrderEntity {
    @Id
    private int id;

    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "mechanic_id")
    private int mechanicId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "work_order_services",
            joinColumns = @JoinColumn(name = "work_order_id")
    )
    @Column(name ="service_item_id")
    private List<Integer> serviceItemIds = new ArrayList<>();

    @Column(name = "status")
    private String status;

    public WorkOrderEntity(){}

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    public int getBookingId(){
        return bookingId;
    }

    public void setBookingId(int bookingId){
        this.bookingId = bookingId;
    }

    public int getMechanicId(){
        return mechanicId;
    }

    public void setMechanicId(int mechanicId){
        this.mechanicId = mechanicId;
    }

    public List<Integer> getServiceItemIds(){
        return serviceItemIds;
    }

    public void setServiceItemIds(List<Integer> serviceItemIds){
        this.serviceItemIds = serviceItemIds;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }
}
