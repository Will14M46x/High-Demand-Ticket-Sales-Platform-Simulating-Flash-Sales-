package com.ticketsale.booking_service;

import com.ticketsale.booking_service.dto.BookingRequest;
import com.ticketsale.booking_service.dto.OrderResponse;
import com.ticketsale.booking_service.dto.QueuePositionResponse;
import com.ticketsale.booking_service.model.Order;
import com.ticketsale.booking_service.model.OrderStatus;
import com.ticketsale.booking_service.repository.OrderRepository;
import com.ticketsale.booking_service.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class BookingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setup() {
        bookingService.inventoryServiceUrl = "http://inventory-service";
        bookingService.waitingRoomServiceUrl = "http://waitingroom-service";
    }

    private QueuePositionResponse admitted(Long userId) {
        QueuePositionResponse qr = new QueuePositionResponse();
        qr.setPosition(0);
        qr.setUserId(String.valueOf(userId));
        qr.setEstimatedWaitTime("0s");
        return qr;
    }

    private QueuePositionResponse notAdmitted(Long userId) {
        QueuePositionResponse qr = new QueuePositionResponse();
        qr.setPosition(5);
        qr.setUserId(String.valueOf(userId));
        qr.setEstimatedWaitTime("30s");
        return qr;
    }

    // -----------------------------------------------------------------------
    @Test
    void testCreateBooking_Success() {

        Long userId = 99L;

        BookingRequest request = new BookingRequest();
        request.setEventId(10L);
        request.setQuantity(2);

        // Only this test needs Redis stubbing
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        when(restTemplate.getForEntity(anyString(), eq(QueuePositionResponse.class)))
                .thenReturn(new ResponseEntity<>(admitted(userId), HttpStatus.OK));

        when(restTemplate.postForEntity(anyString(), isNull(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        Order order = new Order();
        order.setId(1L);
        order.setUserId(userId);
        order.setEventId(10L);
        order.setQuantity(2);
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        when(orderRepository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = bookingService.createBooking(request, userId);

        assertEquals(1L, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
    }

    // -----------------------------------------------------------------------
    @Test
    void testCreateBooking_UserNotAdmitted() {

        Long userId = 50L;

        BookingRequest request = new BookingRequest();
        request.setEventId(5L);
        request.setQuantity(1);

        when(restTemplate.getForEntity(anyString(), eq(QueuePositionResponse.class)))
                .thenReturn(new ResponseEntity<>(notAdmitted(userId), HttpStatus.OK));

        assertThrows(SecurityException.class,
                () -> bookingService.createBooking(request, userId));
    }

    // -----------------------------------------------------------------------
    @Test
    void testCreateBooking_InventoryRejects() {

        Long userId = 99L;

        BookingRequest request = new BookingRequest();
        request.setEventId(10L);
        request.setQuantity(3);

        when(restTemplate.getForEntity(anyString(), eq(QueuePositionResponse.class)))
                .thenReturn(new ResponseEntity<>(admitted(userId), HttpStatus.OK));

        when(restTemplate.postForEntity(anyString(), isNull(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "fail",
                        null, null, null));

        assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request, userId));
    }

    // -----------------------------------------------------------------------
    @Test
    void testConfirmPayment_Success() {

        Order order = new Order();
        order.setId(1L);
        order.setUserId(77L);
        order.setEventId(10L);
        order.setQuantity(2);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("100.00"));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(redisTemplate.hasKey("ticket_hold:1")).thenReturn(true);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = bookingService.confirmPayment(1L);
        assertEquals(OrderStatus.PAID, response.getStatus());
    }

    // -----------------------------------------------------------------------
    @Test
    void testConfirmPayment_Expired() {

        Order order = new Order();
        order.setId(1L);
        order.setEventId(10L);
        order.setQuantity(2);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setUserId(77L);
        order.setTotalAmount(new BigDecimal("100.00"));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(redisTemplate.hasKey("ticket_hold:1")).thenReturn(false);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // inventory release called inside expiration branch
        when(restTemplate.postForEntity(anyString(), isNull(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        OrderResponse response = bookingService.confirmPayment(1L);

        assertEquals(OrderStatus.EXPIRED, response.getStatus());
    }
}