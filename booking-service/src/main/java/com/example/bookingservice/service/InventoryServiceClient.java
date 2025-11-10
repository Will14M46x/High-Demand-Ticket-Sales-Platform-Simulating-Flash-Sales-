package com.example.bookingservice.service;

import com.example.bookingservice.dto.EventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class InventoryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url:http://localhost:8081}")
    private String inventoryServiceUrl;

    public InventoryServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get event details from Inventory Service
     */
    public EventDTO getEvent(Long eventId) {
        try {
            String url = inventoryServiceUrl + "/api/events/" + eventId;
            return restTemplate.getForObject(url, EventDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch event from Inventory Service: " + e.getMessage());
        }
    }

    /**
     * Reserve tickets in Inventory Service
     */
    public EventDTO reserveTickets(Long eventId, int quantity) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(inventoryServiceUrl + "/api/events/reserve/" + eventId)
                    .queryParam("quantity", quantity)
                    .toUriString();

            restTemplate.put(url, null);
            return getEvent(eventId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reserve tickets: " + e.getMessage());
        }
    }

    /**
     * Release tickets back to inventory (if payment fails or hold expires)
     */
    public void releaseTickets(Long eventId, int quantity) {
        try {
            // This would call an endpoint like PUT /api/events/release/{eventId}?quantity=X
            // For now, this is a placeholder - you'd need to implement this endpoint in Inventory Service
            String url = UriComponentsBuilder
                    .fromHttpUrl(inventoryServiceUrl + "/api/events/release/" + eventId)
                    .queryParam("quantity", quantity)
                    .toUriString();
            
            restTemplate.put(url, null);
        } catch (Exception e) {
            // Log error but don't throw - this is a cleanup operation
            System.err.println("Failed to release tickets: " + e.getMessage());
        }
    }
}
