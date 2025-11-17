package com.ticketbooking.authservice.service;

import com.ticketbooking.authservice.model.LoginHistory;
import com.ticketbooking.authservice.model.User;
import com.ticketbooking.authservice.repository.LoginHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoginHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginHistoryService.class);
    
    @Autowired
    private LoginHistoryRepository loginHistoryRepository;
    
    /**
     * Record a successful login
     */
    @Transactional
    public LoginHistory recordLogin(User user, String ipAddress, String userAgent) {
        LoginHistory history = LoginHistory.builder()
            .user(user)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();
        
        // Parse user agent to extract device info
        parseUserAgent(history, userAgent);
        
        // Check if login is suspicious
        checkSuspiciousActivity(history, user, ipAddress);
        
        history = loginHistoryRepository.save(history);
        logger.info("Recorded login for user: {} from IP: {}", user.getEmail(), ipAddress);
        
        return history;
    }
    
    /**
     * Get paginated login history for a user
     */
    @Transactional(readOnly = true)
    public Page<LoginHistory> getUserLoginHistory(User user, Pageable pageable) {
        return loginHistoryRepository.findByUserOrderByLoggedInAtDesc(user, pageable);
    }
    
    /**
     * Get recent login history (last 10 logins)
     */
    @Transactional(readOnly = true)
    public List<LoginHistory> getRecentLogins(User user) {
        return loginHistoryRepository.findTop10ByUserOrderByLoggedInAtDesc(user);
    }
    
    /**
     * Get the most recent login for a user
     */
    @Transactional(readOnly = true)
    public Optional<LoginHistory> getLastLogin(User user) {
        return loginHistoryRepository.findFirstByUserOrderByLoggedInAtDesc(user);
    }
    
    /**
     * Get all suspicious logins for a user
     */
    @Transactional(readOnly = true)
    public List<LoginHistory> getSuspiciousLogins(User user) {
        return loginHistoryRepository.findSuspiciousLoginsByUser(user);
    }
    
    /**
     * Get login count for a user within a time period
     */
    @Transactional(readOnly = true)
    public long getLoginCountSince(User user, LocalDateTime since) {
        return loginHistoryRepository.countLoginsSince(user, since);
    }
    
    /**
     * Get distinct IP addresses used by a user within a time period
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctIpAddresses(User user, LocalDateTime since) {
        return loginHistoryRepository.findDistinctIpAddressesByUserSince(user, since);
    }
    
    /**
     * Get distinct countries from which user logged in within a time period
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctCountries(User user, LocalDateTime since) {
        return loginHistoryRepository.findDistinctCountriesByUserSince(user, since);
    }
    
    /**
     * Parse user agent string to extract device information
     */
    private void parseUserAgent(LoginHistory history, String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            history.setDeviceType("Unknown");
            history.setBrowser("Unknown");
            history.setOperatingSystem("Unknown");
            return;
        }
        
        String ua = userAgent.toLowerCase();
        
        // Detect device type
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            history.setDeviceType("Mobile");
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            history.setDeviceType("Tablet");
        } else {
            history.setDeviceType("Desktop");
        }
        
        // Detect browser
        if (ua.contains("edg/") || ua.contains("edge")) {
            history.setBrowser("Edge");
        } else if (ua.contains("chrome")) {
            history.setBrowser("Chrome");
        } else if (ua.contains("firefox")) {
            history.setBrowser("Firefox");
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            history.setBrowser("Safari");
        } else if (ua.contains("opera") || ua.contains("opr")) {
            history.setBrowser("Opera");
        } else {
            history.setBrowser("Other");
        }
        
        // Detect operating system
        if (ua.contains("windows")) {
            history.setOperatingSystem("Windows");
        } else if (ua.contains("mac os") || ua.contains("macos")) {
            history.setOperatingSystem("MacOS");
        } else if (ua.contains("linux")) {
            history.setOperatingSystem("Linux");
        } else if (ua.contains("android")) {
            history.setOperatingSystem("Android");
        } else if (ua.contains("ios") || ua.contains("iphone") || ua.contains("ipad")) {
            history.setOperatingSystem("iOS");
        } else {
            history.setOperatingSystem("Other");
        }
    }
    
    /**
     * Check if login activity is suspicious
     */
    private void checkSuspiciousActivity(LoginHistory history, User user, String ipAddress) {
        // Get last login
        Optional<LoginHistory> lastLogin = getLastLogin(user);
        
        if (lastLogin.isEmpty()) {
            // First login - not suspicious
            history.setSuspicious(false);
            return;
        }
        
        LoginHistory last = lastLogin.get();
        
        // Check for different country in short time
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        if (last.getLoggedInAt().isAfter(thirtyMinutesAgo)) {
            // Recent login exists
            if (last.getCountry() != null && history.getCountry() != null 
                && !last.getCountry().equals(history.getCountry())) {
                // Different country within 30 minutes - suspicious
                history.setSuspicious(true);
                history.setSuspiciousReason("Login from different country within 30 minutes");
                logger.warn("Suspicious login detected for user {}: Different country", user.getEmail());
                return;
            }
        }
        
        // Check for new IP address
        List<String> recentIps = getDistinctIpAddresses(user, LocalDateTime.now().minusDays(30));
        if (!recentIps.contains(ipAddress) && recentIps.size() > 0) {
            // New IP that hasn't been used in the last 30 days
            history.setSuspicious(true);
            history.setSuspiciousReason("Login from new IP address");
            logger.info("New IP address detected for user {}: {}", user.getEmail(), ipAddress);
            return;
        }
        
        history.setSuspicious(false);
    }
    
    /**
     * Mark a login history entry as reviewed (admin function)
     */
    @Transactional
    public void markAsReviewed(Long historyId) {
        Optional<LoginHistory> history = loginHistoryRepository.findById(historyId);
        if (history.isPresent()) {
            LoginHistory lh = history.get();
            lh.setSuspicious(false);
            lh.setSuspiciousReason(null);
            loginHistoryRepository.save(lh);
            logger.info("Login history {} marked as reviewed", historyId);
        }
    }
}

