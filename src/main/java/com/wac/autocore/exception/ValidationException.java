package com.wac.autocore.exception;

public class ValidationException extends ApplicationException {

    public ValidationException(String messageKey) {
        super(messageKey);
    }
}