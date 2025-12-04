package com.emiraslan.memento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MementoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MementoApplication.class, args);

        System.out.println("Hello and welcome!");
    }
}
