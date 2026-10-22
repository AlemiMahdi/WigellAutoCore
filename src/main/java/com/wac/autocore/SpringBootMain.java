package com.wac.autocore;

import com.wac.autocore.ui.AutoCoreFXApp;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootMain {

    public static void main(String[] args) {
        Application.launch(AutoCoreFXApp.class, args);
    }

}
