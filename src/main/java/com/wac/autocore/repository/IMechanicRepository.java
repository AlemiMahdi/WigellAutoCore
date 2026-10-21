package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;
import java.util.List;

public interface IMechanicRepository {

    Mechanic save(Mechanic mechanic);

    List<Mechanic> findAll();

}
