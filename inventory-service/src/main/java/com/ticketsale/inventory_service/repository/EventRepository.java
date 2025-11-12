package com.ticketsale.inventory_service.repository;

import com.ticketsale.inventory_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// We extend JpaRepository, giving us methods like:
// save(), findById(), findAll(), deleteById(), etc.
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Spring Data JPA automatically understands this method name
    // You can add more custom finders here if needed
}