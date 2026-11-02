package com.wac.autocore.exception;

public class BusinessRuleException extends ApplicationException {

    public BusinessRuleException(String messageKey) {
        super(messageKey);
    }
}