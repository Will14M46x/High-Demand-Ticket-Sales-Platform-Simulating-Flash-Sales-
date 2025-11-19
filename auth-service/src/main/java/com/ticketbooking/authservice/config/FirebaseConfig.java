package com.ticketbooking.authservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.config.path:}")
    private String firebaseConfigPath;
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Authentication will work in mock mode.");
            return null;
        }
        
        // Check if FirebaseApp is already initialized
        if (!FirebaseApp.getApps().isEmpty()) {
            logger.info("FirebaseApp already initialized");
            return FirebaseApp.getInstance();
        }
        
        try {
            FirebaseOptions options;
            
            if (firebaseConfigPath != null && !firebaseConfigPath.isEmpty()) {
                logger.info("Initializing Firebase with config path: {}", firebaseConfigPath);
                
                InputStream serviceAccountStream;
                
                // Check if it's a classpath resource
                if (firebaseConfigPath.startsWith("classpath:")) {
                    String classpathPath = firebaseConfigPath.substring("classpath:".length());
                    logger.info("Loading Firebase config from classpath: {}", classpathPath);
                    serviceAccountStream = resourceLoader.getResource("classpath:" + classpathPath).getInputStream();
                } else {
                    // It's a file path
                    logger.info("Loading Firebase config from file: {}", firebaseConfigPath);
                    serviceAccountStream = new FileInputStream(firebaseConfigPath);
                }
                
                try (InputStream stream = serviceAccountStream) {
                    options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                }
            } else {
                logger.info("No Firebase config path specified, using default credentials");
                // Use default credentials (for cloud deployment)
                options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
            }
            
            FirebaseApp app = FirebaseApp.initializeApp(options);
            logger.info("✅ Firebase initialized successfully!");
            return app;
        } catch (Exception e) {
            logger.error("❌ Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase. Please check your firebase.config.path and ensure the service account JSON file exists.", e);
        }
    }
}

