package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.IMechanicRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MechanicServiceTest {

    @Test
    void readsMechanicsThroughRepository() {
        FakeMechanicRepository repository = new FakeMechanicRepository();
        MechanicService service = new MechanicService(repository);

        List<Mechanic> mechanics = service.findAll();

        assertNotNull(mechanics);
        assertTrue(mechanics.isEmpty());
        assertEquals(1, repository.findAllCalls);
    }

    @Test
    void propagatesStorageFailure() {
        FakeMechanicRepository repository = new FakeMechanicRepository();
        RuntimeException failure =
                new IllegalStateException("Storage unavailable");
        repository.readFailure = failure;

        MechanicService service = new MechanicService(repository);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.findAll()
        );

        assertSame(failure, thrown);
    }

    private static class FakeMechanicRepository
            implements IMechanicRepository {

        private int findAllCalls;
        private RuntimeException readFailure;

        @Override
        public void save(Mechanic mechanic) {
            throw new UnsupportedOperationException(
                    "Saving is not used in these tests"
            );
        }

        @Override
        public List<Mechanic> findAll() {
            findAllCalls++;
            if (readFailure != null) {
                throw readFailure;
            }
            return new ArrayList<>();
        }

        @Override
        public List<Mechanic> findAllMechanics() {
            return findAll();
        }
    }
}