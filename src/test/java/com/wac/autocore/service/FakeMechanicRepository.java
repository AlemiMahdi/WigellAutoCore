package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.IMechanicRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Låtsas-repository för tester (test double / fake).
// Implementerar samma interface som det riktiga MechanicRepository,
// så WorkOrderService märker ingen skillnad. Men inget sparas i MySQL.
public class FakeMechanicRepository implements IMechanicRepository {

    // Kommer ihåg vilken mekaniker som skickades till save.
    // Är den null efter ett test vet vi att inget sparades.
    Mechanic saved;

    @Override
    public void save(Mechanic mechanic) {
        // Ingen databas: vi sparar bara undan objektet så testet kan kontrollera det
        saved = mechanic;
    }

    @Override
    public List<Mechanic> findAll() {
        // Används inte i testerna, returnerar en tom lista
        return new ArrayList<Mechanic>();
    }

    @Override
    public List<Mechanic> findAllMechanics() {
        return new ArrayList<>();
    }
}