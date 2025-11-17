package com.ticketsale.booking_service.repository;

import com.ticketsale.booking_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Spring Data JPA creates all the find/save methods for us
}