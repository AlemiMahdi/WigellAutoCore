package com.wac.autocore.ui;

import com.wac.autocore.exception.BusinessRuleException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.exception.InvoiceValidationException;
import com.wac.autocore.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlerTest {

    private static final String FALLBACK = "fallback.key";

    @Test
    void illegalArgumentIsValidationWithItsOwnKey() {
        RuntimeException exception = new IllegalArgumentException("createBooking.overlapping");

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals("createBooking.overlapping", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void invoiceValidationIsValidation() {
        RuntimeException exception = new InvoiceValidationException("createInvoice.alreadyInvoiced");

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals("createInvoice.alreadyInvoiced", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void validationExceptionIsValidation() {
        RuntimeException exception = new ValidationException("processPayment.selectInvoice");

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals("processPayment.selectInvoice", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void businessRuleExceptionIsValidation() {
        RuntimeException exception = new BusinessRuleException("processPayment.alreadyPaid");

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals("processPayment.alreadyPaid", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void dataAccessExceptionIsTechnicalWithItsOwnKey() {
        RuntimeException exception = new DataAccessException("processPayment.saveError", new RuntimeException());

        assertFalse(ErrorHandler.isValidation(exception));
        assertEquals("processPayment.saveError", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void unexpectedExceptionIsTechnicalWithFallbackKey() {
        RuntimeException exception = new NullPointerException();

        assertFalse(ErrorHandler.isValidation(exception));
        assertEquals(FALLBACK, ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void accessDeniedIsValidationWithSecurityMessage() {
        RuntimeException exception = new AccessDeniedException("Access Denied");

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals("security.accessDenied", ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void validationWithoutMessageUsesFallbackKey() {
        RuntimeException exception = new IllegalArgumentException();

        assertTrue(ErrorHandler.isValidation(exception));
        assertEquals(FALLBACK, ErrorHandler.messageKeyFor(exception, FALLBACK));
    }

    @Test
    void wrappedIllegalArgumentIsUnwrapped() {
        RuntimeException wrapped = new InvalidDataAccessApiUsageException(
                "wrapped", new IllegalArgumentException("editBookingServices.overlap"));

        RuntimeException unwrapped = ErrorHandler.unwrap(wrapped);

        assertTrue(ErrorHandler.isValidation(unwrapped));
        assertEquals("editBookingServices.overlap", ErrorHandler.messageKeyFor(unwrapped, FALLBACK));
    }
}