package com.fredfmelo.orderservice.idempotency.event;

import java.util.UUID;

public interface IdempotentEvent {
    UUID eventId();
    String traceId();
}
