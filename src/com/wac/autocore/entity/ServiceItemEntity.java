package com.wac.autocore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

//Databasrepresentaion av en verkstadstjänst.
@Entity 
@Table (name = "service_items")
public class ServiceItemEntity {
    
    //Samma ID behandling som originalet för att vi fick inte ändra på det.
    @Id 
    private int id;

    @Column (name = "name")
    private String name;

    @Column (name = "description")
    private String description;

    @Column (name = "price")
    private double price;

    @Column (name = "estimated_minutes")
    private int estimatedMinutes;

    //Hibernate behöver en tom konstruktor
    public ServiceItemEntity(){}

    public int getId() { return id;}
    public void setId( int id ) { this.id = id; }

    public String getName() { return name;}
    public void setName(String name ) { this.name = name;}
    
    public String getDescription () { return description; }
    public void setDescription (String description) { this.description = description; }

    public double getPrice() { return price;}
    public void setPrice (double price) { this.price = price; }

    public int getEstimatedMinutes () { return estimatedMinutes; }
    public void setEstimatedMinutes ( int estimatedMinutes ) { this.estimatedMinutes = estimatedMinutes; }




}
