package com.fredfmelo.orderservice.order.event;

import java.time.Instant;
import java.util.UUID;

import com.fredfmelo.orderservice.idempotency.event.IdempotentEvent;

public record PaymentApprovedEvent(UUID eventId,
                String eventType,
                Instant occurredAt,
                String orderId) implements IdempotentEvent {
}