package com.example.inventoryservice.repository;

import com.example.inventoryservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
