package com.github.sps4j.example.springmvc.host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    static {
        System.setProperty("spring.application.admin.enabled", "false");
    }

    public static void main(String[] args) {
       SpringApplication.run(App.class, args);
    }
}

