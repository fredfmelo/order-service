package com.fredfmelo.orderservice.order.listener;

import org.springframework.stereotype.Component;

import com.fredfmelo.orderservice.idempotency.executor.IdempotentExecutor;
import com.fredfmelo.orderservice.order.event.InventoryReservedEvent;
import com.fredfmelo.orderservice.order.service.OrderCommandService;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReservedListener {

    private final OrderCommandService orderCommandService;
    private final IdempotentExecutor idempotentExecutor;

    @SqsListener("${aws.sqs.order-completion-queue}")
    public void consume(InventoryReservedEvent event) {
        idempotentExecutor.execute(event, () -> orderCommandService.complete(event));
    }
}