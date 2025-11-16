package com.ticketsale.inventory_service.service;

import com.ticketsale.inventory_service.dto.CreateEventRequest;
import com.ticketsale.inventory_service.dto.EventResponse;
import com.ticketsale.inventory_service.model.Event;
import com.ticketsale.inventory_service.repository.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Creates a new event in the database.
     */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setTotalTickets(request.getTotalTickets());
        event.setAvailableTickets(request.getTotalTickets());
        event.setSaleStartTime(request.getSaleStartTime());

        // --- UPDATED ---
        // Save the new fields from the frontend
        event.setLocation(request.getLocation());
        event.setPrice(request.getPrice());
        event.setDescription(request.getDescription());
        // ---------------

        Event savedEvent = eventRepository.save(event);
        return mapToEventResponse(savedEvent);
    }

    /**
     * Retrieves a single event by its ID.
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return mapToEventResponse(event);
    }

    /**
     * Retrieves all events.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    /**
     * Atomically reserves tickets for an event using optimistic locking.
     */
    @Transactional
    public boolean reserveTickets(Long eventId, int quantity) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            if (event.getAvailableTickets() >= quantity) {
                event.setAvailableTickets(event.getAvailableTickets() - quantity);
                eventRepository.save(event);
                return true;
            } else {
                return false; // Not enough tickets
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            System.err.println("Optimistic Lock conflict for event: " + eventId);
            return false; // Concurrency failure
        } catch (Exception e) {
            System.err.println("Error during reservation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atomically releases tickets back to an event.
     */
    @Transactional
    public boolean releaseTickets(Long eventId, int quantity) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            int newQuantity = event.getAvailableTickets() + quantity;
            // Safety check: don't release more than the total tickets
            if (newQuantity > event.getTotalTickets()) {
                event.setAvailableTickets(event.getTotalTickets());
            } else {
                event.setAvailableTickets(newQuantity);
            }

            eventRepository.save(event);
            return true;
        } catch (Exception e) {
            System.err.println("Error during ticket release: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an event by its ID.
     */
    @Transactional
    public boolean deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            return false;
        }
        eventRepository.deleteById(id);
        return true;
    }

    /**
     * Helper method to map the Event (Entity) to an EventResponse (DTO).
     */
    private EventResponse mapToEventResponse(Event event) {
        EventResponse dto = new EventResponse();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setTotalTickets(event.getTotalTickets());
        dto.setAvailableTickets(event.getAvailableTickets());
        dto.setSaleStartTime(event.getSaleStartTime());

        // --- UPDATED ---
        // Add the new fields to the response
        dto.setLocation(event.getLocation());
        dto.setPrice(event.getPrice());
        dto.setDescription(event.getDescription());
        // ---------------
        return dto;
    }
}