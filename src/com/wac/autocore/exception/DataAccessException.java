package com.wac.autocore.exception;

public class DataAccessException extends ApplicationException {

    public DataAccessException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }
}