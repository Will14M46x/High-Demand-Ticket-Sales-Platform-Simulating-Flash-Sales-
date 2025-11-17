package com.ticketbooking.common.util;

import com.ticketbooking.common.dto.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Helper utility to extract authenticated user information from request
 * Used by all microservices to get current user context
 */
@Component
public class AuthenticationHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationHelper.class);
    
    /**
     * Extract user context from request attributes
     * These attributes are set by JwtAuthenticationFilter
     * 
     * @param request HTTP request
     * @return UserContext with authenticated user information
     */
    public UserContext getUserContext(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String email = (String) request.getAttribute("email");
            String firebaseUid = (String) request.getAttribute("firebaseUid");
            
            return UserContext.builder()
                    .userId(userId)
                    .email(email)
                    .firebaseUid(firebaseUid)
                    .build();
        } catch (Exception e) {
            logger.error("Failed to extract user context from request: {}", e.getMessage());
            return new UserContext();
        }
    }
    
    /**
     * Get user ID from request
     * 
     * @param request HTTP request
     * @return User ID or null if not authenticated
     */
    public Long getUserId(HttpServletRequest request) {
        return getUserContext(request).getUserId();
    }
    
    /**
     * Get email from request
     * 
     * @param request HTTP request
     * @return Email or null if not authenticated
     */
    public String getEmail(HttpServletRequest request) {
        return getUserContext(request).getEmail();
    }
    
    /**
     * Get Firebase UID from request
     * 
     * @param request HTTP request
     * @return Firebase UID or null if not authenticated
     */
    public String getFirebaseUid(HttpServletRequest request) {
        return getUserContext(request).getFirebaseUid();
    }
}

