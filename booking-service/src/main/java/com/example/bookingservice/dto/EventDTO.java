package com.example.bookingservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    private Long id;
    private String name;
    private String location;
    private String date;
    private int availableTickets;
    private double price;
}
