package com.fredfmelo.orderservice.outbox.publisher;

public interface OutboxEventPublisher {

    void publish(String payload, String eventType);
}