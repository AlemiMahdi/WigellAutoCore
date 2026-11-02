package com.wac.autocore.exception;



public class InvoiceValidationException extends IllegalArgumentException {

    public InvoiceValidationException(String messageKey) {
        super(messageKey);
    }

    public InvoiceValidationException(
            String messageKey,
            Throwable cause) {
        super(messageKey, cause);
    }
}