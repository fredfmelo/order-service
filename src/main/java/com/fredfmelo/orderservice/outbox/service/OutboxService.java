package com.fredfmelo.orderservice.outbox.service;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.orderservice.common.event.Event;
import com.fredfmelo.orderservice.common.exception.TechnicalException;
import com.fredfmelo.orderservice.outbox.entity.OutboxEntity;
import com.fredfmelo.orderservice.outbox.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxService {

    private static final String METADATA = "METADATA";
    private static final String PENDING = "PENDING";
    private static final String OUTBOX_PREFIX = "OUTBOX#";

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(Event event) {
        repository.save(buildEntity(event));
    }

    public OutboxEntity buildEntity(Event event) {

        try {
            OutboxEntity entity = new OutboxEntity();
            String eventId = event.eventId().toString();

            entity.setPk(OUTBOX_PREFIX + eventId);
            entity.setSk(METADATA);

            entity.setEventId(eventId);
            entity.setTraceId(event.traceId());
            entity.setEventType(event.eventType());
            entity.setStatus(PENDING);
            entity.setOutboxStatusPk(OUTBOX_PREFIX + "#" + PENDING);
            entity.setOutboxCreatedAtSk(Instant.now().toString());
            entity.setCreatedAt(Instant.now());

            entity.setPayload(objectMapper.writeValueAsString(event));
            
            return entity;
        } catch (JsonProcessingException ex) {
            throw new TechnicalException("Error serializing outbox payload", ex);
        }
    }
    
}