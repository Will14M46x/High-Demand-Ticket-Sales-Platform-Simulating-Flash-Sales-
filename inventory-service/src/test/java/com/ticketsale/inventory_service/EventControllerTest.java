package com.ticketsale.inventory_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketsale.inventory_service.controller.EventController;
import com.ticketsale.inventory_service.dto.CreateEventRequest;
import com.ticketsale.inventory_service.dto.EventResponse;
import com.ticketsale.inventory_service.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)   // JUnit + Mockito, NO Spring context
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;    // mock the service layer

    @InjectMocks
    private EventController eventController;  // controller under test

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // build MockMvc around JUST the controller (no SecurityConfig, no Jwt, nothing)
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
    }

    @Test
    void testGetAllEvents() throws Exception {
        EventResponse event = new EventResponse();
        event.setId(1L);
        event.setName("Test Event");
        event.setTotalTickets(100);
        event.setAvailableTickets(100);
        event.setLocation("Limerick");
        event.setPrice(new BigDecimal("29.99"));
        event.setDescription("Test description");

        when(eventService.getAllEvents()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/inventory/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Event")));
    }

    @Test
    void testGetEventById() throws Exception {
        EventResponse event = new EventResponse();
        event.setId(5L);
        event.setName("Single Event");

        when(eventService.getEventById(5L)).thenReturn(event);

        mockMvc.perform(get("/api/inventory/events/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.name", is("Single Event")));
    }

    @Test
    void testCreateEvent() throws Exception {
        // request coming from client
        CreateEventRequest request = new CreateEventRequest();
        request.setName("New Event");
        request.setTotalTickets(200);
        request.setLocation("Dublin");
        request.setPrice(new BigDecimal("49.99"));
        request.setDescription("Brand new event");

        // response we expect service to return
        EventResponse response = new EventResponse();
        response.setId(10L);
        response.setName("New Event");
        response.setTotalTickets(200);
        response.setAvailableTickets(200);
        response.setLocation("Dublin");
        response.setPrice(new BigDecimal("49.99"));
        response.setDescription("Brand new event");

        when(eventService.createEvent(any(CreateEventRequest.class))).thenReturn(response);

        mockMvc.perform(
                        post("/api/inventory/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(10)))
                .andExpect(jsonPath("$.name", is("New Event")));
    }
}