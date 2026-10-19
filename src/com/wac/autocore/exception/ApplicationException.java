package com.wac.autocore.exception;

public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String messageKey) {
        super(messageKey);
    }

    protected ApplicationException(String messageKey, Throwable cause) {
        super(messageKey, cause);
    }

    public String getMessageKey() {
        return getMessage();
    }
}