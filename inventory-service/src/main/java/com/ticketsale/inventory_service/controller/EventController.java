package com.ticketsale.inventory_service.controller;

import com.ticketsale.inventory_service.dto.CreateEventRequest;
import com.ticketsale.inventory_service.dto.EventResponse;
import com.ticketsale.inventory_service.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/events") // Base URL for all endpoints in this controller
public class EventController {

    @Autowired
    private EventService eventService;

    // --- CRUD Endpoints ---

    // POST /api/inventory/events
    // Creates a new event
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@RequestBody CreateEventRequest request) {
        return eventService.createEvent(request);
    }

    // GET /api/inventory/events
    // Gets all events
    @GetMapping
    public List<EventResponse> getAllEvents() {
        return eventService.getAllEvents();
    }

    // GET /api/inventory/events/{id}
    // Gets a single event by its ID
    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    // --- CRITICAL Ticket Reservation Endpoint ---

    /**
     * This is the endpoint the Booking/Order Service will call.
     * POST /api/inventory/events/{id}/reserve
     */
    @PostMapping("/{id}/reserve")
    public ResponseEntity<String> reserveTickets(
            @PathVariable Long id,
            @RequestParam int quantity) {

        boolean success = eventService.reserveTickets(id, quantity);

        if (success) {
            return ResponseEntity.ok("Tickets reserved successfully.");
        } else {
            // 409 Conflict is a good status code for a concurrency failure
            // or "not enough inventory"
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Failed to reserve tickets (e.g., inventory lock or insufficient stock).");
        }
    }
}