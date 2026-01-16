package io.github.sps4j.example.springmvc.plugin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;

public class TestService implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;


    @Override
    public void run(String... args) throws Exception {
        System.out.println("test service load!!! " + applicationContext);
    }

    public void test() {
        System.out.println("test service test!!!" + this);
    }
}