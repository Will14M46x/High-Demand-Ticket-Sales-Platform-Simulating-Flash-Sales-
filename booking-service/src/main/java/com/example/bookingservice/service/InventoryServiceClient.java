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
            String url = inventoryServiceUrl + "/api/inventory/events/" + eventId;
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
                    .fromHttpUrl(inventoryServiceUrl + "/api/inventory/events/" + eventId + "/reserve")
                    .queryParam("quantity", quantity)
                    .toUriString();

            EventDTO response = restTemplate.postForObject(url, null, EventDTO.class);
            if (response == null) {
                throw new RuntimeException("Inventory service returned empty response when reserving tickets.");
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reserve tickets: " + e.getMessage());
        }
    }

    /**
     * Release tickets back to inventory (if payment fails or hold expires)
     */
    public void releaseTickets(Long eventId, int quantity) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(inventoryServiceUrl + "/api/inventory/events/" + eventId + "/release")
                    .queryParam("quantity", quantity)
                    .toUriString();

            restTemplate.postForLocation(url, null);
        } catch (Exception e) {
            // Log error but don't throw - this is a cleanup operation
            System.err.println("Failed to release tickets: " + e.getMessage());
        }
    }
}
