package com.ticketsale.inventory_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// This DTO is used when a client wants to CREATE a new event
@Data
public class CreateEventRequest {
    private String name;
    private Integer totalTickets;
    private LocalDateTime saleStartTime;
    private String location;
    private BigDecimal price;
    private String description;
}