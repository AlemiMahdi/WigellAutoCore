package com.wac.autocore.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BookingOverlapCheckerTest {
    private final LocalDate date = LocalDate.of(2026, 11, 2);

    @Test
    void sameTimeSameMechanicOverlaps() {
        // Arrange: mekaniker 1 är bokad 09:00-10:00
        List<Booking> bookings = List.of(booking(1, date, LocalTime.of(9, 0), 60));
        LocalDateTime newStart = LocalDateTime.of(date, LocalTime.of(9, 0));
        LocalDateTime newEnd = LocalDateTime.of(date, LocalTime.of(10, 0));

        // Act : försök boka mekaniker 1 samma tid
        boolean result = BookingOverlapChecker.hasOverlap(bookings, 1, newStart, newEnd);

        // Assert: det ska vara krock
        assertTrue(result);
    }

    @Test
    void otherMechanicDoesNotOverlap() {
        // Arrange: Mekaniker 2 är bokad 09:00-10:00
        List<Booking> bookings = List.of(booking(2, date, LocalTime.of(9, 0), 60));
        LocalDateTime newStart = LocalDateTime.of(date, LocalTime.of(9, 0));
        LocalDateTime newEnd = LocalDateTime.of(date, LocalTime.of(10, 0));

        // Act: Försök boka mekaniker 1 samma tid
        boolean result = BookingOverlapChecker.hasOverlap(bookings, 1, newStart, newEnd);

        // Assert: Ingen krock, olika mekaniker
        assertFalse(result);
    }

    @Test
    void startsInsideExistingOverlaps() {
        // Arrange: Mekaniker 1 är bokad 09:00-10:00
        List<Booking> bookings = List.of(booking(1, date, LocalTime.of(9, 0), 60));
        LocalDateTime newStart = LocalDateTime.of(date, LocalTime.of(9, 30));
        LocalDateTime newEnd = LocalDateTime.of(date, LocalTime.of(10, 30));

        // Act: Försöker boka överlappande tid
        boolean result = BookingOverlapChecker.hasOverlap(bookings, 1, newStart, newEnd);

        // Assert: Det ska krocka
        assertTrue(result);
    }

    @Test
    void startsWhenExistingEndDoesNotOverlap() {
        // Arrange: Mekaniker 1 är bokad 09:00-10:00
        List<Booking> bookings = List.of(booking(1, date, LocalTime.of(9, 0), 60));
        LocalDateTime newStart = LocalDateTime.of(date, LocalTime.of(10, 0));
        LocalDateTime newEnd = LocalDateTime.of(date, LocalTime.of(11, 0));

        // Act: Boka en ny tid 10:00-11:00
        boolean result = BookingOverlapChecker.hasOverlap(bookings, 1, newStart, newEnd);

        // Assert: Ska inte krocka
        assertFalse(result);

    }

    private Booking booking(int mechanicId, LocalDate date, LocalTime startTime, int minutes) {
        Booking booking = new Booking(0, 1, date, startTime, minutes, "test");

        booking.setMechanicId(mechanicId);
        return booking;
    }
}
