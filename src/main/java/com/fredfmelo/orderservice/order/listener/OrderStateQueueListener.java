package com.fredfmelo.orderservice.order.listener;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.eventdrivencore.idempotency.executor.IdempotentExecutor;
import com.fredfmelo.orderservice.order.event.InventoryReservedEvent;
import com.fredfmelo.orderservice.order.event.InventoryUnavailableEvent;
import com.fredfmelo.orderservice.order.event.PaymentApprovedEvent;
import com.fredfmelo.orderservice.order.event.PaymentRefundedEvent;
import com.fredfmelo.orderservice.order.service.OrderCommandService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStateQueueListener {

    private static final String EVENT_TYPE = "eventType";

    private final ObjectMapper objectMapper;
    private final IdempotentExecutor idempotentExecutor;
    private final OrderCommandService orderCommandService;

    @SqsListener("${aws.sqs.order-state-queue}")
    public void consume(List<Message<String>> messages) {
        for (Message<String> message : messages) {
            handleMessage(message.getPayload());
        }
    }

    private void handleMessage(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.get(EVENT_TYPE).asText();

            switch (eventType) {
                case "PAYMENT_APPROVED" -> {
                    PaymentApprovedEvent event = objectMapper.readValue(payload, PaymentApprovedEvent.class);
                    idempotentExecutor.execute(event, () -> orderCommandService.approvePayment(event));
                }
                case "PAYMENT_REFUNDED" -> {
                    PaymentRefundedEvent event = objectMapper.readValue(payload, PaymentRefundedEvent.class);
                    idempotentExecutor.execute(event, () -> orderCommandService.refundPayment(event));
                }
                case "INVENTORY_RESERVED" -> {
                    InventoryReservedEvent event = objectMapper.readValue(payload, InventoryReservedEvent.class);
                    idempotentExecutor.execute(event, () -> orderCommandService.complete(event));
                }
                case "INVENTORY_UNAVAILABLE" -> {
                    InventoryUnavailableEvent event = objectMapper.readValue(payload, InventoryUnavailableEvent.class);
                    idempotentExecutor.execute(event, () -> orderCommandService.failOrder(event));
                }
                default -> throw new IllegalArgumentException("Unsupported event type: " + eventType);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to process order state event", e);
        }
    }
}
