package com.ticketbooking.authservice.repository;

import com.ticketbooking.authservice.model.LoginHistory;
import com.ticketbooking.authservice.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    
    Page<LoginHistory> findByUserOrderByLoggedInAtDesc(User user, Pageable pageable);
    
    List<LoginHistory> findTop10ByUserOrderByLoggedInAtDesc(User user);
    
    Optional<LoginHistory> findFirstByUserOrderByLoggedInAtDesc(User user);
    
    @Query("SELECT lh FROM LoginHistory lh WHERE lh.user = :user AND lh.suspicious = true ORDER BY lh.loggedInAt DESC")
    List<LoginHistory> findSuspiciousLoginsByUser(User user);
    
    @Query("SELECT COUNT(lh) FROM LoginHistory lh WHERE lh.user = :user AND lh.loggedInAt > :since")
    long countLoginsSince(User user, LocalDateTime since);
    
    @Query("SELECT DISTINCT lh.ipAddress FROM LoginHistory lh WHERE lh.user = :user AND lh.loggedInAt > :since")
    List<String> findDistinctIpAddressesByUserSince(User user, LocalDateTime since);
    
    @Query("SELECT DISTINCT lh.country FROM LoginHistory lh WHERE lh.user = :user AND lh.loggedInAt > :since")
    List<String> findDistinctCountriesByUserSince(User user, LocalDateTime since);
}

