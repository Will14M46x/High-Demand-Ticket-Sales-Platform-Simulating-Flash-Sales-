package com.ticketsale.inventory_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// This DTO is what we RETURN to the client
@Data
public class EventResponse {
    private Long id;
    private String name;
    private Integer totalTickets;
    private Integer availableTickets;
    private LocalDateTime saleStartTime;
    private String location;
    private BigDecimal price;
    private String description;
}