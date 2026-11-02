package com.wac.autocore.standalone;

import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.IServiceItemRepository;

import java.util.List;

/**
 * Tjänsterepo för det fristående programmet (WAC-106).
 * Används inte av operationerna, men BookingService kräver det i konstruktorn.
 * Alla metoder kastar UnsupportedOperationException.
 * Ingen @Repository här, annars hittar Spring två repon i Main 1.
 */
public class InMemoryServiceItemRepository implements IServiceItemRepository {

    private static final String NOT_USED = "Not used in standalone program";

    @Override
    public ServiceItem save(ServiceItem serviceItem) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<ServiceItem> findAll() {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public ServiceItem findById(int id) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public List<ServiceItem> findAllByIds(List<Integer> ids) {
        throw new UnsupportedOperationException(NOT_USED);
    }

    @Override
    public void update(ServiceItem serviceItem) {
        throw new UnsupportedOperationException(NOT_USED);

    }
}
