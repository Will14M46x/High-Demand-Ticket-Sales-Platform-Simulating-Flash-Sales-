package com.ticketbooking.authservice.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
    
    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;
    
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
}

