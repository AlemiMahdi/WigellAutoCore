package com.wac.autocore.ui;

import com.wac.autocore.exception.ApplicationException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.Label;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class ErrorHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {
    }

    public static void show(
            Label label,
            RuntimeException exception,
            String fallbackKey) {

        String messageKey = fallbackKey;

        if (exception instanceof ApplicationException) {
            messageKey =
                    ((ApplicationException) exception).getMessageKey();
        }

        if (exception instanceof DataAccessException
                || !(exception instanceof ApplicationException)) {
            String errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            LOGGER.error("Operation failed: {}(error id {})", messageKey, errorId, exception);

            LanguageManager language = LanguageManager.getInstance();
            StringBinding message = language.text(messageKey);
            StringBinding codeTemplate = language.text("error.code");

            label.textProperty().unbind();
            UiKit.showError(label, "");
            label.textProperty().bind(Bindings.createStringBinding(
                    () -> message.get() + " " + String.format(codeTemplate.get(), errorId),
                    message, codeTemplate
            ));
            return;

        }

        label.textProperty().unbind();
        UiKit.showError(label, "");
        label.textProperty().bind(
                LanguageManager.getInstance().text(messageKey)
        );
    }
}