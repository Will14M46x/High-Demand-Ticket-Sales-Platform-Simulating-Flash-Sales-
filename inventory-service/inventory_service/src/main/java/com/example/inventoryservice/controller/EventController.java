// src/main/java/com/example/inventoryservice/controller/EventController.java
package com.example.inventoryservice.controller;

import com.example.inventoryservice.model.Event;
import com.example.inventoryservice.repository.EventRepository;
import com.example.inventoryservice.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;



@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final InventoryService inventoryService;
    private final EventRepository eventRepository;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public EventController(InventoryService inventoryService, EventRepository eventRepository, JdbcTemplate jdbc, DataSource dataSource) {
        this.inventoryService = inventoryService;
        this.eventRepository = eventRepository;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/debug/events-flat-count")
    public Map<String, Object> countFlat() {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM events_flat", Integer.class);
        return Map.of("count", c);
    }

    @GetMapping("/debug/datasource")
    public Map<String, Object> dsInfo() {
        try (var conn = dataSource.getConnection()) {
            var meta = conn.getMetaData();
            return Map.of(
                    "url", meta.getURL(),
                    "user", meta.getUserName(),
                    "driver", meta.getDriverName()
            );
        } catch (Exception e) {
            return Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", e.getMessage()
            );
        }
    }



    @PostMapping("/debug/insert-flat")
    public Map<String, Object> insertFlat() {
        int rows = jdbc.update(
                "INSERT INTO events_flat (name, location, date, available_tickets, price) VALUES (?,?,?,?,?)",
                "Debug Concert", "Dublin", "2025-12-01", 5, 49.99
        );
        return Map.of("inserted", rows);
    }

    @PostMapping
    public Event createEvent(@RequestBody Event event) {
        // id is auto-generated; ignore incoming id if present
        event.setId(null);
        return eventRepository.save(event);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long id, @RequestBody Event updated) {
        return eventRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setLocation(updated.getLocation());
                    existing.setDate(updated.getDate());
                    existing.setAvailableTickets(updated.getAvailableTickets());
                    existing.setPrice(updated.getPrice());
                    return ResponseEntity.ok(eventRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!eventRepository.existsById(id)) return ResponseEntity.notFound().build();
        eventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Demo reserve endpoint using your service logic
    @PostMapping("/{id}/reserve")
    public ResponseEntity<Event> reserve(@PathVariable Long id, @RequestParam int qty) {
        Event e = inventoryService.reserveTickets(id, qty);
        if (e == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(e);
    }
}
