package com.emiraslan.memento.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // return the current one if it's working to prevent errors
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // reads json from /resources for key
        InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("serviceAccountKey.json");

        if (serviceAccount == null) {
            log.warn("⚠️ serviceAccountKey.json not found in resources! Notifications will not work.");
            return null;
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}