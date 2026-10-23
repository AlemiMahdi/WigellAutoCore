package com.wac.autocore.ui;

import com.wac.autocore.exception.ApplicationException;
import com.wac.autocore.exception.DataAccessException;
import com.wac.autocore.ui.language.LanguageManager;
import javafx.scene.control.Label;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ErrorHandler {

    private static final Logger LOGGER =
            Logger.getLogger(ErrorHandler.class.getName());

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
            LOGGER.log(Level.SEVERE, "Operation failed", exception);
        }

        label.textProperty().unbind();
        UiKit.showError(label, "");
        label.textProperty().bind(
                LanguageManager.getInstance().text(messageKey)
        );
    }
}