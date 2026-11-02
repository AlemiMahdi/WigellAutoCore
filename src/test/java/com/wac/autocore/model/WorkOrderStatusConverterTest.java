package com.wac.autocore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testar att gamla statusvärden från WAC 2.5 läses in rätt i den nya modellen (WAC-66).
public class WorkOrderStatusConverterTest {

    // I 2.5 hette en skapad men inte started order "CREATED".
    // I 3.0 heter samma sak CONFIRMED.
    @Test
    void oldCreatedBecomesConfirmed() {
        WorkOrderStatusConverter converter = new WorkOrderStatusConverter();

        WorkOrderStatus result = converter.convertToEntityAttribute("CREATED");

        assertEquals(WorkOrderStatus.CONFIRMED, result);
    }

    @Test
    void oldInProgressStaysInProgress() {
        WorkOrderStatusConverter converter = new WorkOrderStatusConverter();

        WorkOrderStatus result = converter.convertToEntityAttribute("IN_PROGRESS");

        assertEquals(WorkOrderStatus.IN_PROGRESS, result);

    }

    @Test
    void oldCompletedStaysCompleted() {
        WorkOrderStatusConverter converter = new WorkOrderStatusConverter();

        WorkOrderStatus result = converter.convertToEntityAttribute("COMPLETED");

        assertEquals(WorkOrderStatus.COMPLETED, result);
    }

    @Test
    void nullBecomesConfirmed() {
        WorkOrderStatusConverter converter = new WorkOrderStatusConverter();

        WorkOrderStatus result = converter.convertToEntityAttribute(null);

        assertEquals(WorkOrderStatus.CONFIRMED, result);
    }

    @Test
    void unknownValueBecomesConfirmed() {
        WorkOrderStatusConverter converter = new WorkOrderStatusConverter();

        WorkOrderStatus result = converter.convertToEntityAttribute("XYZ");

        assertEquals(WorkOrderStatus.CONFIRMED, result);
    }
}
