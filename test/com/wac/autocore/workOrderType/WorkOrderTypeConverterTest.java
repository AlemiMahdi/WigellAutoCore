package com.wac.autocore.workOrderType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testar att gamla arbetsordrar utan typ (WAC 2.5) läses och sparas som PLANNED (WAC-66).
public class WorkOrderTypeConverterTest {

    @Test
    void nullTypeBecomesPlanned() {
        WorkOrderTypeConverter converter = new WorkOrderTypeConverter();

        WorkOrderTypeEnum result = converter.convertToEntityAttribute(null);

        assertEquals(WorkOrderTypeEnum.PLANNED, result);
    }

    @Test
    void emptyTypeBecomesPlanned() {
        WorkOrderTypeConverter converter = new WorkOrderTypeConverter();

        WorkOrderTypeEnum result = converter.convertToEntityAttribute("");

        assertEquals(WorkOrderTypeEnum.PLANNED, result);
    }

    @Test
    void dropInReadCorrectly() {
        WorkOrderTypeConverter converter = new WorkOrderTypeConverter();

        WorkOrderTypeEnum result = converter.convertToEntityAttribute("DROP_IN");

        assertEquals(WorkOrderTypeEnum.DROP_IN, result);
    }

    @Test
    void unknownTypeBecomesPlanned() {
        WorkOrderTypeConverter converter = new WorkOrderTypeConverter();

        WorkOrderTypeEnum result = converter.convertToEntityAttribute("YXZ");

        assertEquals(WorkOrderTypeEnum.PLANNED, result);
    }

    @Test
    void savingNullTypeWritesPlanned() {
        WorkOrderTypeConverter converter = new WorkOrderTypeConverter();

        String result = converter.convertToDatabaseColumn(null);

        assertEquals("PLANNED", result);
    }
}
