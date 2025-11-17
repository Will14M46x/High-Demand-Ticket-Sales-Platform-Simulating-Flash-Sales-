package com.ticketbooking.authservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${firebase.config.path:}")
    private String firebaseConfigPath;
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Authentication will work in mock mode.");
            return null;
        }
        
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                FirebaseOptions options;
                
                if (firebaseConfigPath != null && !firebaseConfigPath.isEmpty()) {
                    logger.info("Loading Firebase config from: {}", firebaseConfigPath);
                    
                    // Use ResourceLoader to handle classpath: prefix properly
                    Resource resource = resourceLoader.getResource(firebaseConfigPath);
                    
                    if (!resource.exists()) {
                        throw new IOException("Firebase config file not found: " + firebaseConfigPath);
                    }
                    
                    // Use try-with-resources to ensure InputStream is always closed
                    try (InputStream serviceAccount = resource.getInputStream()) {
                        options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                    }
                } else {
                    logger.info("No Firebase config path specified, using application default credentials");
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
                logger.warn("Firebase authentication will not be available");
                return null;
            }
        }
        logger.info("Firebase already initialized, returning existing instance");
        return FirebaseApp.getInstance();
    }
}

