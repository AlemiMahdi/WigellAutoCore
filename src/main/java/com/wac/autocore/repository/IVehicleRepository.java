package com.wac.autocore.repository;

import com.wac.autocore.model.Vehicle;

import java.util.List;

public interface IVehicleRepository {

     Vehicle save(Vehicle vehicle);

     List<Vehicle> findAll();

     Vehicle findById(int id);
}
