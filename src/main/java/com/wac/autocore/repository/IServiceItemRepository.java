package com.wac.autocore.repository;

import com.wac.autocore.model.ServiceItem;

import java.util.List;

public interface IServiceItemRepository {
    ServiceItem save(ServiceItem serviceItem);
    List<ServiceItem> findAll();
    ServiceItem findById(int id);
    List<ServiceItem> findAllByIds(List<Integer> ids);
    void update(ServiceItem serviceItem);
}
