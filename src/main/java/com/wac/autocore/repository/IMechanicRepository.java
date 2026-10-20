package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;
import java.util.List;

public interface IMechanicRepository {

    void save(Mechanic mechanic);

    List<Mechanic> findAll();

    List<Mechanic> findAllMechanics();

}
