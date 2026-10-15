package com.wac.autocore.repository;

import com.wac.autocore.model.ServicePackage;

import java.util.List;

public interface IServicePackageRepository {

    void save(ServicePackage servicePackage);

    List<ServicePackage> findAll();
}
