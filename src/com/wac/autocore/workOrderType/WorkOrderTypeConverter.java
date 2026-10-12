package com.wac.autocore.workOrderType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true) // autoApply gör att Hibernate använder denna automatiskt
public class WorkOrderTypeConverter implements AttributeConverter<WorkOrderTypeEnum, String> {

    // Vad som ska sparas i databasen (används när du sparar nya orders)
    @Override
    public String convertToDatabaseColumn(WorkOrderTypeEnum attribute) {
        if (attribute == null) {
            return "PLANNED";
        }
        return attribute.name();
    }

    // Vad som händer när Hibernate LÄSER från databasen (skyddar historisk data!)
    @Override
    public WorkOrderTypeEnum convertToEntityAttribute(String dbData) {
        // Om kolumnen är null, tom ("") eller innehåller något okänt: returnera PLANNED
        if (dbData == null || dbData.trim().isEmpty()) {
            return WorkOrderTypeEnum.PLANNED;
        }

        try {
            return WorkOrderTypeEnum.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return WorkOrderTypeEnum.PLANNED;
        }
    }
}