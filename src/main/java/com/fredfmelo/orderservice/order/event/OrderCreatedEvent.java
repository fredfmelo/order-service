package com.fredfmelo.orderservice.order.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fredfmelo.orderservice.idempotency.event.IdempotentEvent;

public record OrderCreatedEvent(UUID eventId,
                String eventType,
                Instant occurredAt,
                String orderId,
                UUID customerId,
                List<OrderItemEvent> items) implements IdempotentEvent {
}