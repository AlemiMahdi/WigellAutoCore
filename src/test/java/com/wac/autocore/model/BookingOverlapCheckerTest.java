package com.wac.autocore.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

public class BookingOverlapCheckerTest {
    private final LocalDate date = LocalDate.of(2026, 11, 2);

    @Test
    void sameTimeSameMechanicOverlaps() {
        // Arrange: mekaniker 1 är bokad 09:00-10:00
        List<Booking>  bookings = List.of(booking(1, date, LocalTime.of(9, 0), 60));
        LocalDateTime newStart = LocalDateTime.of(date, LocalTime.of(9,0));
        LocalDateTime newEnd = LocalDateTime.of(date, LocalTime.of(10, 0));

        // Act : försök boka mekaniker 1 samma tid
        boolean result = BookingOverlapChecker.hasOverlap(bookings, 1, newStart, newEnd);

        // Assert: det ska vara krock
        assertTrue(result);
    }

    private Booking booking(int mechanicId, LocalDate date, LocalTime startTime, int minutes) {
        Booking booking = new Booking(0, 1, date, startTime, minutes, "test");

        booking.setMechanicId(mechanicId);
        return booking;
    }
}
