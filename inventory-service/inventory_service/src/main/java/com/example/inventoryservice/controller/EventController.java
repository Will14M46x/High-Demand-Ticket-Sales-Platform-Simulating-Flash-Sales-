package com.example.inventoryservice.controller;

import com.example.inventoryservice.service.InventoryService;
import com.example.inventoryservice.model.Event;
import com.example.inventoryservice.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final InventoryService inventoryService;

    public EventController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Autowired
    private EventRepository eventRepository;

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public Event getEventById(@PathVariable Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody Event updatedEvent) {
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

    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        eventRepository.deleteById(id);
    }

    @PutMapping("/reserve/{eventId}")
    public ResponseEntity<?> reserveTicket(@PathVariable Long eventId,
                                           @RequestParam(defaultValue = "1") int quantity) {
        Event updated = inventoryService.reserveTickets(eventId, quantity);

        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Not enough tickets available or event not found.");
        }

        return ResponseEntity.ok(updated);
    }
    @PutMapping("/release/{eventId}")
    public ResponseEntity<?> releaseTickets(@PathVariable Long eventId,
                                            @RequestParam(defaultValue = "1") int quantity) {
        Event updated = inventoryService.releaseTickets(eventId, quantity);

        if (updated == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Event not found.");
        }

        return ResponseEntity.ok(updated);
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Inventory service is running!";
    }


}


