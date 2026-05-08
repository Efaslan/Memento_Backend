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
        System.out.println("⚠️ Notifications will not work unless you have a serviceAccountKey.json in resources. Please check FirebaseConfig.java class. ⚠️");

        // RedisCacheWarmer starts on ApplicationReadyEvent
    }

    // Setting the timezone with @PostConstruct shows wrong logging times. I directly set the Digital Ocean VM's timezone into Europe/Istanbul
    // Todo: update LocalDateTimes into Instants for different timezones
}
