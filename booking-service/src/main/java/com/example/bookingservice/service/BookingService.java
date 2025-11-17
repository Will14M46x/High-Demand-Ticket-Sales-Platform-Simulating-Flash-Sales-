package com.example.bookingservice.service;

import com.example.bookingservice.dto.*;
import com.example.bookingservice.model.Order;
import com.example.bookingservice.model.OrderStatus;
import com.example.bookingservice.model.TicketHold;
import com.example.bookingservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final HoldService holdService;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    public BookingService(HoldService holdService,
                          InventoryServiceClient inventoryServiceClient,
                          PaymentService paymentService,
                          OrderRepository orderRepository) {
        this.holdService = holdService;
        this.inventoryServiceClient = inventoryServiceClient;
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
    }

    /**
     * Step 1: Create a booking (reserve tickets and create hold)
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        try {
            // 1. Get event details
            EventDTO event = inventoryServiceClient.getEvent(request.getEventId());
            if (event == null) {
                return BookingResponse.builder()
                        .status("FAILED")
                        .message("Event not found")
                        .build();
            }

            // 2. Check if enough tickets are available
            if (event.getAvailableTickets() < request.getQuantity()) {
                return BookingResponse.builder()
                        .status("FAILED")
                        .message("Not enough tickets available")
                        .build();
            }

            // 3. Calculate total price
            double totalPrice = event.getPrice() * request.getQuantity();

            // 4. Reserve tickets in Inventory Service
            EventDTO updatedEvent = inventoryServiceClient.reserveTickets(
                    request.getEventId(), 
                    request.getQuantity()
            );
            
            if (updatedEvent == null) {
                return BookingResponse.builder()
                        .status("FAILED")
                        .message("Failed to reserve tickets")
                        .build();
            }

            // 5. Create hold in Redis
            TicketHold hold = holdService.createHold(
                    request.getEventId(),
                    request.getUserId(),
                    request.getQuantity(),
                    totalPrice
            );

            // 6. Create pending order in database
            Order order = Order.builder()
                    .eventId(request.getEventId())
                    .userId(request.getUserId())
                    .quantity(request.getQuantity())
                    .totalPrice(totalPrice)
                    .status(OrderStatus.PENDING)
                    .expiresAt(hold.getExpiresAt())
                    .build();
            
            order = orderRepository.save(order);

            return BookingResponse.builder()
                    .holdId(hold.getHoldId())
                    .orderId(order.getId())
                    .eventId(request.getEventId())
                    .quantity(request.getQuantity())
                    .totalPrice(totalPrice)
                    .expiresAt(hold.getExpiresAt())
                    .status("SUCCESS")
                    .message("Tickets reserved. Complete payment within 10 minutes.")
                    .build();

        } catch (Exception e) {
            return BookingResponse.builder()
                    .status("FAILED")
                    .message("Booking failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Step 2: Complete booking with payment
     */
    @Transactional
    public PaymentResponse completeBooking(PaymentRequest paymentRequest) {
        try {
            // 1. Validate hold
            TicketHold hold = holdService.getHold(paymentRequest.getHoldId());
            if (hold == null || !hold.isActive()) {
                // Hold expired or invalid - release tickets
                Order order = orderRepository.findById(paymentRequest.getOrderId()).orElse(null);
                if (order != null) {
                    order.setStatus(OrderStatus.EXPIRED);
                    orderRepository.save(order);
                    inventoryServiceClient.releaseTickets(order.getEventId(), order.getQuantity());
                }
                
                return PaymentResponse.builder()
                        .status("FAILED")
                        .message("Booking hold has expired")
                        .orderId(paymentRequest.getOrderId())
                        .build();
            }

            // 2. Get order
            Order order = orderRepository.findById(paymentRequest.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            // 3. Update order status to processing
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            // 4. Process payment
            PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

            // 5. Handle payment result
            if ("SUCCESS".equals(paymentResponse.getStatus())) {
                // Payment successful
                order.setStatus(OrderStatus.COMPLETED);
                order.setPaymentId(paymentResponse.getPaymentId());
                order.setCompletedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Release hold from Redis
                holdService.releaseHold(paymentRequest.getHoldId());

                return PaymentResponse.builder()
                        .paymentId(paymentResponse.getPaymentId())
                        .status("SUCCESS")
                        .message("Payment successful! Tickets confirmed.")
                        .orderId(order.getId())
                        .build();

            } else {
                // Payment failed - release tickets and hold
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

                holdService.releaseHold(paymentRequest.getHoldId());
                inventoryServiceClient.releaseTickets(order.getEventId(), order.getQuantity());

                return PaymentResponse.builder()
                        .status("FAILED")
                        .message(paymentResponse.getMessage())
                        .orderId(order.getId())
                        .build();
            }

        } catch (Exception e) {
            return PaymentResponse.builder()
                    .status("FAILED")
                    .message("Payment processing failed: " + e.getMessage())
                    .orderId(paymentRequest.getOrderId())
                    .build();
        }
    }

    /**
     * Cancel a booking
     */
    @Transactional
    public boolean cancelBooking(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            if (order.getStatus() == OrderStatus.COMPLETED) {
                throw new RuntimeException("Cannot cancel completed order");
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Release tickets back to inventory
            inventoryServiceClient.releaseTickets(order.getEventId(), order.getQuantity());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get order details
     */
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    /**
     * Get all orders for a user
     */
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * Cleanup expired holds (called by scheduled task)
     */
    @Transactional
    public void cleanupExpiredHolds() {
        List<Order> expiredOrders = orderRepository.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING, 
                LocalDateTime.now()
        );

        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            // Release tickets back to inventory
            inventoryServiceClient.releaseTickets(order.getEventId(), order.getQuantity());
        }
    }
}
