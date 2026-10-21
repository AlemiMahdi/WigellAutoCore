package com.wac.autocore;

import com.wac.autocore.ui.AutoCoreFXApp;
import com.wac.autocore.ui.views.ShowWorkOrdersView;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class SpringBootMain {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        SpringApplication.launch(AutoCoreFXApp.class, args);
    }

}
