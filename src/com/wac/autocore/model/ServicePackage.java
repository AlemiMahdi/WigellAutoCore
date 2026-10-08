package com.wac.autocore.model;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

@Entity 
@Table (name = "service_packages")
public class ServicePackage {

    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private int id;

    @Column (name = "name", nullable = false)
    private String name;

    @ManyToMany (fetch = FetchType.EAGER)
    @JoinTable (
        name = "service_package_services",
        joinColumns = @JoinColumn ( name = "package_id"),
        inverseJoinColumns = @JoinColumn ( name = "service_id")
    )
    private List<ServiceItem> services = new ArrayList<>();

    public ServicePackage() {}
    public ServicePackage(String name) { this.name = name; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name;}
    public void setName(String name) { this.name = name; }

    public List<ServiceItem> getServices() { return services; }
    public void setServices( List<ServiceItem> services ) { this.services = services; }

    public void addService( ServiceItem service ) { services.add(service); }
    public void removeService( ServiceItem service ) { services.remove(service); }
    
    @Override 
    public String toString() { return name; }
}
