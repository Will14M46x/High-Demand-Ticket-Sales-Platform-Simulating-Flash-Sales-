// src/main/java/com/example/inventoryservice/model/Event.java
package com.example.inventoryservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "events_flat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String location;

    // For quick demo, keep as String; you can switch to LocalDate later
    private String date;

    @Column(name = "available_tickets")
    private int availableTickets;

    private double price;
}
