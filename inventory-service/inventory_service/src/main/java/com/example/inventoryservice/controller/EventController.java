package com.example.inventoryservice.controller;

import com.example.inventoryservice.service.InventoryService;
import com.example.inventoryservice.model.Event;
import com.example.inventoryservice.repository.EventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/events")
@CrossOrigin(origins = "*")
public class EventController {
    
    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final InventoryService inventoryService;

    @Autowired
    private EventRepository eventRepository;

    public EventController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // -------------------------------
    // CRUD ENDPOINTS FOR EVENTS
    // -------------------------------

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    /**
     * Create a new event (requires authentication)
     */
    @PostMapping
    public Event createEvent(@RequestBody Event event, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) creating event: {}", email, userId, event.getName());
        return eventRepository.save(event);
    }

    /**
     * Update an existing event (requires authentication)
     */
    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Event updatedEvent, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) updating event ID: {}", email, userId, id);
        
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            event.setName(updatedEvent.getName());
            event.setLocation(updatedEvent.getLocation());
            event.setDate(updatedEvent.getDate());
            event.setAvailableTickets(updatedEvent.getAvailableTickets());
            event.setPrice(updatedEvent.getPrice());
            return eventRepository.save(event);
        }
        return null;
    }

    /**
     * Delete an event (requires authentication)
     */
    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) deleting event ID: {}", email, userId, id);
        eventRepository.deleteById(id);
    }

    /**
     * Reserve tickets for an event (requires authentication)
     */
    @PutMapping("/reserve/{eventId}")
    public ResponseEntity<?> reserveTicket(@PathVariable Long eventId,
                                           @RequestParam(defaultValue = "1") int quantity,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) reserving {} ticket(s) for event ID: {}", email, userId, quantity, eventId);
        
        Event updated = inventoryService.reserveTickets(eventId, quantity);

        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Not enough tickets available or event not found.");
        }

        return ResponseEntity.ok(updated);
    }
    /**
     * Release tickets back to inventory (requires authentication)
     * Used when a booking is cancelled or hold expires
     */
    @PutMapping("/release/{eventId}")
    public ResponseEntity<?> releaseTickets(@PathVariable Long eventId,
                                            @RequestParam(defaultValue = "1") int quantity,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String email = (String) request.getAttribute("email");
        logger.info("User {} (ID: {}) releasing {} ticket(s) for event ID: {}", email, userId, quantity, eventId);
        
        Event updated = inventoryService.releaseTickets(eventId, quantity);

        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Event not found.");
        }

        return ResponseEntity.ok(updated);
    }

    // -------------------------------
    // HEALTH CHECK
    // -------------------------------

    @GetMapping("/health")
    public String healthCheck() {
        return "Inventory service is running!";
                }

}