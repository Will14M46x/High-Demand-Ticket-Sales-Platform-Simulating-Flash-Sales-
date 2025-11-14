package com.ticketbooking.authservice.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    
    @Value("${firebase.api.key:}")
    private String firebaseApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public UserRecord createUser(String email, String password, String displayName) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Returning mock user.");
            return null;
        }
        
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName)
                    .setEmailVerified(false)
                    .setDisabled(false);
            
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            logger.info("Successfully created Firebase user: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            logger.error("Error creating Firebase user: {}", e.getMessage());
            throw e;
        }
    }
    
    public FirebaseToken verifyToken(String firebaseToken) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Returning null token.");
            return null;
        }
        
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
            logger.info("Successfully verified Firebase token for user: {}", decodedToken.getUid());
            return decodedToken;
        } catch (FirebaseAuthException e) {
            logger.error("Error verifying Firebase token: {}", e.getMessage());
            throw e;
        }
    }
    
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Returning null user.");
            return null;
        }
        
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            logger.info("Successfully retrieved Firebase user: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            logger.error("Error retrieving Firebase user by email: {}", e.getMessage());
            throw e;
        }
    }
    
    public UserRecord getUserByUid(String uid) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Returning null user.");
            return null;
        }
        
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            logger.info("Successfully retrieved Firebase user: {}", userRecord.getUid());
            return userRecord;
        } catch (FirebaseAuthException e) {
            logger.error("Error retrieving Firebase user by UID: {}", e.getMessage());
            throw e;
        }
    }
    
    public void deleteUser(String uid) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. User deletion skipped.");
            return;
        }
        
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
            logger.info("Successfully deleted Firebase user: {}", uid);
        } catch (FirebaseAuthException e) {
            logger.error("Error deleting Firebase user: {}", e.getMessage());
            throw e;
        }
    }
    
    public String createCustomToken(String uid) throws FirebaseAuthException {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Returning mock token.");
            return "mock-firebase-token";
        }
        
        try {
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);
            logger.info("Successfully created custom token for user: {}", uid);
            return customToken;
        } catch (FirebaseAuthException e) {
            logger.error("Error creating custom token: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Verifies user credentials (email and password) using Firebase Authentication REST API
     * This is required because Firebase Admin SDK doesn't provide server-side password verification
     * 
     * @param email User's email
     * @param password User's password
     * @return Firebase ID token if authentication succeeds
     * @throws FirebaseAuthException if authentication fails or Firebase is misconfigured
     */
    public String verifyPassword(String email, String password) {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Skipping password verification.");
            // In test/dev mode, we'll accept any password for existing users
            return null;
        }
        
        if (firebaseApiKey == null || firebaseApiKey.isEmpty()) {
            logger.error("Firebase API key is not configured. Cannot verify password.");
            throw new RuntimeException("Firebase API key is not configured. Set firebase.api.key in application.properties");
        }
        
        try {
            // Prepare request body for Firebase REST API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Call Firebase Authentication REST API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                FIREBASE_AUTH_URL + firebaseApiKey,
                request,
                Map.class
            );
            
            if (response.getBody() != null && response.getBody().containsKey("idToken")) {
                String idToken = (String) response.getBody().get("idToken");
                logger.info("Successfully verified password for user: {}", email);
                return idToken;
            } else {
                logger.error("Firebase authentication response missing idToken");
                throw new RuntimeException("Invalid response from Firebase: missing idToken");
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Firebase password verification failed for {}: {}", email, e.getMessage());
            
            // Parse Firebase error response
            if (e.getStatusCode().value() == 400) {
                throw new RuntimeException("Invalid email or password");
            } else if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Invalid email or password");
            } else {
                throw new RuntimeException("Authentication failed: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Unexpected error during password verification: {}", e.getMessage());
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
}

