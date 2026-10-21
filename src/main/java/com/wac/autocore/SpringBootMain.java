package com.wac.autocore;

import com.wac.autocore.ui.AutoCoreFXApp;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootMain {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        SpringApplication.launch(AutoCoreFXApp.class, args);
    }

}
