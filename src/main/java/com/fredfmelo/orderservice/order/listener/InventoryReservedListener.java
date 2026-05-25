package com.fredfmelo.orderservice.order.listener;

import org.springframework.stereotype.Component;

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

    @SqsListener("${aws.sqs.order-completion-queue}")
    public void consume(InventoryReservedEvent event) {

        log.info(
                "Inventory reserved order={}",
                event.orderId()
        );

        orderCommandService.complete(event);
    }
}