package com.emiraslan.memento.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credential.path}")
    private Resource firebaseCredential;

    @Bean
    public FirebaseApp firebaseApp() {
        // return the current one if it's working to prevent errors
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        if (firebaseCredential == null || !firebaseCredential.exists()) {
            log.warn("⚠️ Firebase credential file not found at provided path! Notifications will not work. ⚠️");
            return null;
        }

        try (InputStream serviceAccount = firebaseCredential.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            log.info("Firebase successfully started.");

            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: ", e);
            return null;
        }
    }
}