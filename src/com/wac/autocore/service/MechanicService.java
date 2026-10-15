package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.IMechanicRepository;

import java.util.List;
import java.util.Objects;

public class MechanicService {

    private final IMechanicRepository mechanicRepository;

    public MechanicService(IMechanicRepository mechanicRepository) {
        this.mechanicRepository = Objects.requireNonNull(
                mechanicRepository, "Mechanic repository is required"
        );
    }

    public void save(Mechanic mechanic) {
        Objects.requireNonNull(mechanic, "Mechanic is required");
        mechanicRepository.save(mechanic);
    }

    public List<Mechanic> findAll() {
        return mechanicRepository.findAll();
    }
}