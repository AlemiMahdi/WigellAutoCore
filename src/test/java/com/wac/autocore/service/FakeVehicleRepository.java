package com.wac.autocore.service;

import com.wac.autocore.data.Database;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.IVehicleRepository;

import java.util.Collections;
import java.util.List;

public class FakeVehicleRepository implements IVehicleRepository {
    @Override
    public Vehicle save(Vehicle vehicle) {
        return vehicle;
    }

    @Override
    public List<Vehicle> findAll() {
        return Collections.emptyList();
    }

    @Override
    public Vehicle findById(int id) {
        return Database.getVehicles().stream()
                .filter(vehicle -> vehicle.getId() == id)
                .findFirst()
                .orElse(null);
    }
}
