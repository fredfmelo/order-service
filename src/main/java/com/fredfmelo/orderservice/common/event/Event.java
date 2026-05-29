package com.fredfmelo.orderservice.common.event;

import java.util.UUID;

public interface Event {

    UUID eventId();
    String traceId();
    String eventType();

    default String logContext() {
        return "traceId=%s, eventId=%s, eventType=%s".formatted(
                        traceId(),
                        eventId(),
                        eventType()
                );
    }
}