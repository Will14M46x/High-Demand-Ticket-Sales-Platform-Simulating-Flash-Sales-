package com.ticketbooking.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;
    
    @PostConstruct
    public void init() {
        // Check if using insecure development secret
        if (secret != null && (secret.contains("INSECURE") || 
                               secret.contains("TEST-ONLY") || 
                               secret.contains("CHANGE-ME") ||
                               secret.equals("ticketBookingSecretKeyForAuthenticationServiceMustBeAtLeast256Bits"))) {
            logger.warn("═══════════════════════════════════════════════════════════════");
            logger.warn("⚠️  SECURITY WARNING: Using insecure JWT secret!");
            logger.warn("⚠️  Current secret contains: {}", 
                       secret.substring(0, Math.min(30, secret.length())) + "...");
            logger.warn("⚠️  This is ONLY acceptable for local development/testing.");
            logger.warn("⚠️  NEVER use this in production!");
            logger.warn("⚠️  Set JWT_SECRET environment variable with a secure value.");
            logger.warn("⚠️  Generate one with: openssl rand -base64 32");
            logger.warn("═══════════════════════════════════════════════════════════════");
        } else {
            logger.info("JWT utility initialized with secure secret");
        }
        
        logger.info("JWT token expiration set to: {} ms ({} hours)", 
                   expiration, expiration / 3600000);
    }
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String generateToken(String email, Long userId, String firebaseUid) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("firebaseUid", firebaseUid);
        return createToken(claims, email);
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
    
    public String extractFirebaseUid(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("firebaseUid", String.class);
    }
    
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    public Boolean validateToken(String token, String email) {
        final String extractedEmail = extractEmail(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token));
    }
    
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}

