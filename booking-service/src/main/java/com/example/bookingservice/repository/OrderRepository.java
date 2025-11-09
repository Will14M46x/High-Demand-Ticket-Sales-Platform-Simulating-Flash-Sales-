package com.example.bookingservice.repository;

import com.example.bookingservice.model.Order;
import com.example.bookingservice.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime time);
}
