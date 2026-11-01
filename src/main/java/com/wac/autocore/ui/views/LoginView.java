
package com.wac.autocore.ui.views;

import com.wac.autocore.security.AuthenticationService;
import com.wac.autocore.ui.UiKit;
import com.wac.autocore.ui.language.LanguageManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.springframework.security.core.AuthenticationException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginView extends VBox {

    private static final Logger LOGGER =
            Logger.getLogger(LoginView.class.getName());

    private final AuthenticationService authenticationService;
    private final Runnable onLoginSuccess;

    private final LanguageManager language =
            LanguageManager.getInstance();

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();

    private final Label feedback = UiKit.feedbackLabel();

    public LoginView(
            AuthenticationService authenticationService,
            Runnable onLoginSuccess) {

        this.authenticationService = authenticationService;
        this.onLoginSuccess = onLoginSuccess;

        setAlignment(Pos.CENTER);
        setSpacing(16);
        setPadding(new Insets(30));

        Label title = new Label();
        title.textProperty().bind(language.text("login.title"));

        title.setStyle(
                "-fx-font-size: 28px;" +
                "-fx-font-weight: bold;"
        );

        usernameField.promptTextProperty().bind(
                language.text("login.username")
        );

        passwordField.promptTextProperty().bind(
                language.text("login.password")
        );

        usernameField.setMaxWidth(320);
        passwordField.setMaxWidth(320);

        Button loginButton = UiKit.primaryButton("Login");
        loginButton.textProperty().bind(
                language.text("login.button")
        );

        loginButton.setOnAction(event -> login());

        // Användaren kan också logga in med Enter.
        passwordField.setOnAction(event -> login());

        getChildren().addAll(
                title,
                usernameField,
                passwordField,
                loginButton,
                feedback
        );
    }

    private void login() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            UiKit.showError(
                    feedback,
                    language.text("login.missingFields").get()
            );
            return;
        }

        try {
            authenticationService.login(username, password);

            passwordField.clear();

            // Öppna huvudapplikationen vid lyckad inloggning.
            onLoginSuccess.run();

        } catch (AuthenticationException exception) {

            passwordField.clear();

            UiKit.showError(
                    feedback,
                    language.text("login.invalidCredentials").get()
            );

        } catch (RuntimeException exception) {

            passwordField.clear();

            LOGGER.log(
                    Level.SEVERE,
                    "Unexpected login error",
                    exception
            );

            UiKit.showError(
                    feedback,
                    language.text("login.unexpectedError").get()
            );
        }
    }
}
