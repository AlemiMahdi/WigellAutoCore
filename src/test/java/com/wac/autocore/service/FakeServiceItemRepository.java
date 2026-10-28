package com.wac.autocore.service;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IServiceItemRepository;

import java.util.List;

public class FakeServiceItemRepository implements IServiceItemRepository {
    @Override
    public ServiceItem save(ServiceItem serviceItem) {
        return null;
    }

    @Override
    public List<ServiceItem> findAll() {
        return List.of();
    }

    @Override
    public ServiceItem findById(int id) {
        return null;
    }

    @Override
    public void update(ServiceItem serviceItem) {
    }
}
