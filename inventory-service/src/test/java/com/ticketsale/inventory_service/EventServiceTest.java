package com.ticketsale.inventory_service;

import com.ticketsale.inventory_service.dto.CreateEventRequest;
import com.ticketsale.inventory_service.dto.EventResponse;
import com.ticketsale.inventory_service.model.Event;
import com.ticketsale.inventory_service.repository.EventRepository;
import com.ticketsale.inventory_service.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventRepository eventRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Event buildEvent() {
        Event e = new Event();
        e.setId(1L);
        e.setName("Test Event");
        e.setTotalTickets(100);
        e.setAvailableTickets(100);
        e.setSaleStartTime(LocalDateTime.now());
        e.setLocation("Dublin");
        e.setPrice(new BigDecimal("50.00"));
        e.setDescription("Test description");
        return e;
    }

    // --- CREATE ---
    @Test
    void testCreateEvent() {
        CreateEventRequest req = new CreateEventRequest();
        req.setName("Test Event");
        req.setTotalTickets(100);
        req.setSaleStartTime(LocalDateTime.now());
        req.setLocation("Dublin");
        req.setPrice(new BigDecimal("50.00"));
        req.setDescription("Description");

        Event saved = buildEvent();

        when(eventRepository.save(any(Event.class))).thenReturn(saved);

        EventResponse res = eventService.createEvent(req);

        assertNotNull(res);
        assertEquals("Test Event", res.getName());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    // --- GET BY ID ---
    @Test
    void testGetEventById() {
        Event event = buildEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventResponse res = eventService.getEventById(1L);

        assertEquals(1L, res.getId());
    }

    @Test
    void testGetEventById_NotFound() {
        when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> eventService.getEventById(99L));
    }

    // --- GET ALL ---
    @Test
    void testGetAllEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(buildEvent()));

        List<EventResponse> list = eventService.getAllEvents();

        assertEquals(1, list.size());
    }

    // --- RESERVE TICKETS ---
    @Test
    void testReserveTickets_Success() {
        Event event = buildEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        boolean result = eventService.reserveTickets(1L, 10);

        assertTrue(result);
        assertEquals(90, event.getAvailableTickets());
    }

    @Test
    void testReserveTickets_NotEnough() {
        Event event = buildEvent();
        event.setAvailableTickets(5);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        boolean result = eventService.reserveTickets(1L, 10);

        assertFalse(result);
    }

    @Test
    void testReserveTickets_OptimisticLock() {
        when(eventRepository.findById(1L))
                .thenThrow(new ObjectOptimisticLockingFailureException(Event.class, 1L));

        boolean result = eventService.reserveTickets(1L, 5);

        assertFalse(result);
    }

    // --- RELEASE ---
    @Test
    void testReleaseTickets() {
        Event event = buildEvent();
        event.setAvailableTickets(50);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        boolean result = eventService.releaseTickets(1L, 5);

        assertTrue(result);
        assertEquals(55, event.getAvailableTickets());
    }

    // --- DELETE ---
    @Test
    void testDeleteEvent() {
        when(eventRepository.existsById(1L)).thenReturn(true);

        boolean result = eventService.deleteEvent(1L);

        assertTrue(result);
        verify(eventRepository, times(1)).deleteById(1L);
    }
}