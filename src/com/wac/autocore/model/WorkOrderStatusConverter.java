package com.wac.autocore.model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

// Översätter arbetsorderns status mellan Java (enum) och databasen (text),
// Skydda gammal data: arbetsordrar sparade före kan läsas utan krasch.
@Converter
public class WorkOrderStatusConverter implements AttributeConverter<WorkOrderStatus, String> {

    // Körs när vi spara: enum -> text, t.ex. CONFIRMED -> "CONFORMED".
    @Override
    public String convertToDatabaseColumn(WorkOrderStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    // Körs när vi läser: text -> enum
    @Override
    public WorkOrderStatus convertToEntityAttribute(String dbData) {
        // Gamla systemet hade "CREATED" = skapad men inte startad
        // Heter CONFIRMED  i nya. Tomt värde blir också CONFIRMED.
        if (dbData == null || dbData.trim().isEmpty() || dbData.equals("CREATED")) {
            return WorkOrderStatus.CONFIRMED;
        }

        try {
            return WorkOrderStatus.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            return WorkOrderStatus.CONFIRMED;
        }
    }
}
