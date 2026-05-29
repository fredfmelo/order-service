package com.fredfmelo.orderservice.outbox.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fredfmelo.orderservice.common.exception.TechnicalException;
import com.fredfmelo.orderservice.outbox.entity.OutboxEntity;
import com.fredfmelo.orderservice.outbox.publisher.OutboxEventPublisher;
import com.fredfmelo.orderservice.outbox.repository.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private static final String PROCESSED = "PROCESSED";
    private static final String PROCESSED_STATUS_PK = "OUTBOX_STATUS#PROCESSED";

    private final OutboxRepository repository;
    private final OutboxEventPublisher publisher;

    @Scheduled(fixedDelay = 5000)
    public void process() {
        var pendingEvents = repository.findPending();

        if (!pendingEvents.isEmpty()) {
            log.info("Found {} pending outbox events", pendingEvents.size());
        }

        pendingEvents.forEach(this::processEvent);
    }

    private void processEvent(OutboxEntity event) {
        long startedAt = System.currentTimeMillis();

        String eventLog = "traceId={}, eventId={}, eventType={}".formatted(event.getTraceId(),
        event.getEventId(),
        event.getEventType());

        try {
            log.info("Publishing event: {}", eventLog);

            publisher.publish(event.getPayload(), event.getEventType());

            updateAndSaveOutbox(event);

            long durationMs = System.currentTimeMillis() - startedAt;

            log.info("Event published: {}, durationMs={}", eventLog, durationMs);

        } catch (Exception ex) {
            throw new TechnicalException("Error publishing event: " + eventLog, ex);
        }
    }

    private void updateAndSaveOutbox(OutboxEntity event) {
        event.setStatus(PROCESSED);
        event.setOutboxStatusPk(PROCESSED_STATUS_PK);

        repository.save(event);
    }
}