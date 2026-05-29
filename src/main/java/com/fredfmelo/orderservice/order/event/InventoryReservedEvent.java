package com.fredfmelo.orderservice.order.event;

import java.time.Instant;
import java.util.UUID;

import com.fredfmelo.eventdrivencore.event.Event;

public record InventoryReservedEvent(UUID eventId,
        String traceId,
        String eventType,
        Instant occurredAt,
        String orderId) implements Event{
}