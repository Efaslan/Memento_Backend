package com.emiraslan.memento;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MementoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MementoApplication.class, args);

        System.out.println("Hello and welcome!");
        System.out.println("⚠️ Notifications will not work unless you have a serviceAccountKey.json in resources. Please check FirebaseConfig.java class. ⚠️");

        // RedisCacheWarmer starts on ApplicationReadyEvent
    }

    // Fixing the timezone to Istanbul as we will only have Turkish users for now.
    // Todo: update LocalDateTimes into Instants for different timezones
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Istanbul"));
    }
}
