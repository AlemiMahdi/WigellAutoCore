package com.wac.autocore.model;

import java.time.LocalDateTime;
import java.util.List;

public class BookingOverlapChecker {

    public static boolean hasOverlap(List<Booking> bookings, int mechanicId,
                                     LocalDateTime newStart, LocalDateTime newEnd) {
        for (Booking booking : bookings) {

            if (booking.getMechanicId() == null || booking.getMechanicId() != mechanicId) {
                continue;
            }

            if (booking.getDate() == null
                    || booking.getStartTime() == null
                    || booking.getDurationMinutes() <= 0) {
                continue;
            }

            LocalDateTime existingStart = LocalDateTime.of(
                    booking.getDate(),
                    booking.getStartTime()
            );
            LocalDateTime existingEnd = existingStart.plusMinutes(booking.getDurationMinutes());

            if (newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart)) {
                return true;
            }
        }
        return false;
    }

}
