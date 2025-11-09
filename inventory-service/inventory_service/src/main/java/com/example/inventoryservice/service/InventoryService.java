package com.example.inventoryservice.service;

import com.example.inventoryservice.model.Event;
import com.example.inventoryservice.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final EventRepository eventRepository;

    public InventoryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public synchronized Event reserveTickets(Long eventId, int quantity) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || event.getAvailableTickets() < quantity) {
            return null;
        }

        event.setAvailableTickets(event.getAvailableTickets() - quantity);
        return eventRepository.save(event);
    }
}
