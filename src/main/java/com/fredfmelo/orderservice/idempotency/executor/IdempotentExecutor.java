package com.fredfmelo.orderservice.idempotency.executor;

import org.springframework.stereotype.Component;

import com.fredfmelo.orderservice.common.event.Event;
import com.fredfmelo.orderservice.common.exception.TechnicalException;
import com.fredfmelo.orderservice.idempotency.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentExecutor {

    private final IdempotencyService idempotencyService;

    public void execute(Event event, Runnable action) {
        long startedAt = System.currentTimeMillis();
        String eventLog = event.logContext();        

        if (!idempotencyService.acquire(event.eventId())) {
            log.info("Duplicate event ignored: {}", eventLog);
            return;
        }

        try {
            log.info("Event received: {}", eventLog);

            action.run();
            idempotencyService.markProcessed(event.eventId());

            long durationMs = System.currentTimeMillis() - startedAt;

            log.info("Event processed: {}, durationMs={}",
             eventLog,
             durationMs);

        } catch (Exception ex) {
            throw new TechnicalException("Error processing event: "+ eventLog, ex);
        }
    }
}