package com.wac.autocore.repository;

import com.wac.autocore.model.Vehicle;

import java.util.List;

public interface IVehicleRepository {

    public void save(Vehicle vehicle);

    public List<Vehicle> findAll();

    public Vehicle findById(int id);
}
