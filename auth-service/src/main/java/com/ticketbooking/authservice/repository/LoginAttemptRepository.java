package com.ticketbooking.authservice.repository;

import com.ticketbooking.authservice.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    List<LoginAttempt> findByEmailAndAttemptedAtAfterOrderByAttemptedAtDesc(String email, LocalDateTime after);
    
    List<LoginAttempt> findByIpAddressAndAttemptedAtAfterOrderByAttemptedAtDesc(String ipAddress, LocalDateTime after);
    
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsSince(String email, LocalDateTime since);
    
    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ipAddress AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsByIpSince(String ipAddress, LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    int deleteOldAttempts(LocalDateTime before);
}

