package com.wac.autocore.ui.language;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public final class LanguageManager {

    // samma språkhanterare används av alla vyer
    private static final LanguageManager INSTANCE = new LanguageManager();

    private final ObjectProperty<ResourceBundle> translations  = new SimpleObjectProperty<>();

    private LanguageManager() {
        setLanguage("sv");
    }

    public static LanguageManager getInstance() {
        return INSTANCE;
    }

    // Läser in filen och uppdaterar alla bundna texter
    public void setLanguage(String language) {
        if (!"sv".equals(language) && (!"en".equals(language))) {
            throw new IllegalArgumentException(
                    "unsupported language: " + language
            );
        }

        String fileName = "/messages_" + language + ".properties";

        InputStream in = LanguageManager.class.getResourceAsStream(fileName);

        if (in == null) {
            throw new IllegalStateException(
                    "Could not load properties file: " + fileName
            );
        }

        // UTF-8 gör så att å, ä och ö  läses korrekt
        try (InputStreamReader reader = new InputStreamReader(in,
                StandardCharsets.UTF_8)) {

            ResourceBundle loaded = new PropertyResourceBundle(reader);
            translations.set(loaded);

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load properties file: " + fileName, exception
            );

        }
    }

    // texten räknas automatiskt när språkfilen byts.
    public StringBinding text(String key) {
        return Bindings.createStringBinding(() -> translations.get().getString(key), translations);
    }
}
