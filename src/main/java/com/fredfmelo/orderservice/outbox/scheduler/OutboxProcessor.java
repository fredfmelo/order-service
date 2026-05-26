package com.fredfmelo.orderservice.outbox.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fredfmelo.orderservice.infrastructure.messaging.OrderEventPublisher;
import com.fredfmelo.orderservice.outbox.entity.OutboxEntity;
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
    private final OrderEventPublisher publisher;

    @Scheduled(fixedDelay = 5000)
    public void process() {
        var pendingOrders = repository.findPending();

        log.info("[SCHEDULER] Outbox scheduler found {} orders to proccess ", pendingOrders.size());

        for (OutboxEntity event : pendingOrders) {
            try {
                publisher.publishRaw(event.getPayload(), event.getEventType());

                updateAndSaveOutbox(event);

                log.info("Outbox processed={}", event.getPk());

            } catch (Exception ex) {
                log.error("Error processing outbox={}", event.getPk(), ex);
            }
        }
    }

    private void updateAndSaveOutbox(OutboxEntity event) {
        event.setStatus(PROCESSED);
        event.setOutboxStatusPk(PROCESSED_STATUS_PK);

        repository.save(event);
    }
}