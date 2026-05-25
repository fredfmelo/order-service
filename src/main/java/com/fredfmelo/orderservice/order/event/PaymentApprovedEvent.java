package com.fredfmelo.orderservice.order.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentApprovedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String orderId
) {
}