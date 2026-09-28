package com.wac.autocore.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

//Databasrepresentation av en mekaniker
@Entity 
@Table (name = "mechanics")
public class MechanicEntity {
    
    //Samma ID hantering som originalsystemet
    @Id 
    private int id;

    @Column (name = "name")
    private String name;

    @Column (name = "phone")
    private String phone;

    @Column (name = "specialization")
    private String specialization;

    @Column (name = "available")
    private boolean available;

    public MechanicEntity() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName ( String name) { this.name = name; }
    
    public String getPhone () { return phone; }
    public void setPhone ( String phone) { this.phone = phone; }

    public String getSpecialization () { return specialization; }
    public void setSpecialization ( String specialization ) { this.specialization = specialization; }

    public boolean isAvailable () { return available; }
    public void setAvailable ( boolean available) { this.available = available; }
    


}
