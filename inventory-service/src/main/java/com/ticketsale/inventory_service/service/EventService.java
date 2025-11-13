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

    // --- CRUD Operations (as required by spec) ---

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.getName());
        event.setTotalTickets(request.getTotalTickets());
        // When creating, available tickets = total tickets
        event.setAvailableTickets(request.getTotalTickets());
        event.setSaleStartTime(request.getSaleStartTime());
        // Version starts at 0 automatically

        Event savedEvent = eventRepository.save(event);
        return mapToEventResponse(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return mapToEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean deleteEvent(Long id) {
        // First, check if the event even exists
        if (!eventRepository.existsById(id)) {
            // It doesn't exist, so we can't delete it
            return false;
        }

        // It does exist, so we delete it
        eventRepository.deleteById(id);

        // Return true to signal success
        return true;
    }

    // --- CRITICAL Concurrency-Safe Logic ---

    /**
     * Attempts to reserve a specified number of tickets for an event.
     * This method is transactional and uses optimistic locking.
     * @param eventId The ID of the event
     * @param quantity The number of tickets to reserve
     * @return true if reservation was successful, false otherwise
     */
    @Transactional
    public boolean reserveTickets(Long eventId, int quantity) {
        try {
            // 1. Find the event
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            // 2. Check availability
            if (event.getAvailableTickets() >= quantity) {
                // 3. Decrement inventory
                event.setAvailableTickets(event.getAvailableTickets() - quantity);

                // 4. Save
                // JPA will automatically check the @Version field.
                // If the version has changed since we read it (step 1),
                // it will throw ObjectOptimisticLockingFailureException.
                eventRepository.save(event);

                System.out.println("Reservation successful for " + quantity + " tickets.");
                return true;
            } else {
                // Not enough tickets
                System.out.println("Reservation failed: Not enough tickets.");
                return false;
            }
        } catch (ObjectOptimisticLockingFailureException e) {
            // --- CONCURRENCY FAILURE ---
            // This is NOT an error. It's a feature.
            // It means another user's transaction completed first.
            // The Booking Service must handle this (e.g., by retrying or failing the order)
            System.err.println("Optimistic Lock conflict for event: " + eventId);
            return false;
        } catch (Exception e) {
            System.err.println("Error during reservation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to release a specified number of tickets back to the inventory.
     * This is also transactional and uses optimistic locking.
     * @param eventId The ID of the event
     * @param quantity The number of tickets to release
     * @return true if release was successful, false otherwise
     */
    @Transactional
    public boolean releaseTickets(Long eventId, int quantity) {
        try {
            // 1. Find the event
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new EntityNotFoundException("Event not found"));

            // 2. Add the quantity back to the available pool
            // We can also check to make sure we don't release more than total
            int newQuantity = event.getAvailableTickets() + quantity;
            if (newQuantity > event.getTotalTickets()) {
                // This is a safety check, it shouldn't really happen
                event.setAvailableTickets(event.getTotalTickets());
            } else {
                event.setAvailableTickets(newQuantity);
            }

            // 3. Save (JPA will check the @Version)
            eventRepository.save(event);

            System.out.println("Release successful for " + quantity + " tickets.");
            return true;

        } catch (ObjectOptimisticLockingFailureException e) {
            // Concurrency failure. This is rare on a release, but possible.
            System.err.println("Optimistic Lock conflict during release for event: " + eventId);
            return false;
        } catch (Exception e) {
            System.err.println("Error during ticket release: " + e.getMessage());
            return false;
        }
    }

    // --- Helper Method ---

    // Utility method to map the Event (Entity) to an EventResponse (DTO)
    private EventResponse mapToEventResponse(Event event) {
        EventResponse dto = new EventResponse();
        dto.setId(event.getId());
        dto.setName(event.getName());
        dto.setTotalTickets(event.getTotalTickets());
        dto.setAvailableTickets(event.getAvailableTickets());
        dto.setSaleStartTime(event.getSaleStartTime());
        return dto;
    }
}