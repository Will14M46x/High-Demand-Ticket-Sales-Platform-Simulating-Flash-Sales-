package com.ticketsale.inventory_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data // Lombok: auto-generates getters, setters, toString, etc.
@NoArgsConstructor // Lombok: auto-generates a no-argument constructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    // This is the CRITICAL field for inventory management
    @Column(name = "available_tickets", nullable = false)
    private Integer availableTickets;

    @Column(name = "sale_start_time", nullable = false)
    private LocalDateTime saleStartTime;

    // This is the key to preventing overselling (Optimistic Locking)
    // It maps to the 'version' column we created in SQL
    @Version
    private Integer version;

    @Column
    private String location;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 1000)
    private String description;
}