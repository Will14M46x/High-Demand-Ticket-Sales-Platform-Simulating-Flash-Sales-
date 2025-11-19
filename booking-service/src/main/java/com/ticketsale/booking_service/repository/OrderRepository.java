package com.ticketsale.booking_service.repository;

import com.ticketsale.booking_service.model.Order;
import com.ticketsale.booking_service.model.OrderStatus; // Import OrderStatus
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoffTime);
}