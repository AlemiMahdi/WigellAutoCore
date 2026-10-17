package com.wac.autocore.repository;

import com.wac.autocore.model.ServiceItem;

import java.util.List;

public interface IServiceItemRepository {
    void save(ServiceItem serviceItem);
    List<ServiceItem> findAll();
    ServiceItem findById(int id);
}
