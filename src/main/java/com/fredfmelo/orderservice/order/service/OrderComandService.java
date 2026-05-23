package com.fredfmelo.orderservice.order.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.fredfmelo.orderservice.infrastructure.messaging.OrderEventPublisher;
import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.model.OrderItem;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;
import com.fredfmelo.orderservice.order.domain.OrderStatus;
import com.fredfmelo.orderservice.order.event.OrderCreatedEvent;
import com.fredfmelo.orderservice.order.event.OrderItemEvent;
import com.fredfmelo.orderservice.order.repository.OrderEntityRepository;
import com.fredfmelo.orderservice.order.repository.OrderItemEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderComandService {

    private final OrderEntityRepository orderEntityRepository;
    private final OrderItemEntityRepository orderItemEntityRepository;

    private final OrderEventPublisher orderEventPublisher;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validateRequest(request);

        OrderEntity orderEntity = createAndSaveOrder(request);
        List<OrderItemEntity> items = createAndSaveOrderItem(request.getItems(), orderEntity.getPk());

        buildSnsEventAndPublish(orderEntity, items);

        CreateOrderResponse response = new CreateOrderResponse();
        return response;
    }

    private void buildSnsEventAndPublish(OrderEntity orderEntity, List<OrderItemEntity> items) {
        List<OrderItemEvent> itemEvents = items.stream()
                .map(item -> new OrderItemEvent(item.getProductId(), item.getQuantity()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "ORDER_CREATED",
                Instant.now(),
                orderEntity.getPk().replace("ORDER#", ""),
                orderEntity.getCustomerId(),
                itemEvents);

        orderEventPublisher.publish(event);
    }

    private OrderEntity createAndSaveOrder(CreateOrderRequest request) {
        String pk = "ORDER#" + UUID.randomUUID();

        var orderEntity = OrderEntity.builder()
                .pk(pk)
                .sk("METADATA")
                .status(OrderStatus.CREATED)
                .customerId(request.getCustomerId())
                .build();

        orderEntityRepository.save(orderEntity);

        return orderEntity;
    }

    private List<OrderItemEntity> createAndSaveOrderItem(List<OrderItem> orderItemList, String orderPk) {
        AtomicInteger itemIndex = new AtomicInteger(1);
        List<OrderItemEntity> items = new ArrayList<>();


        orderItemList.forEach(item -> {
            OrderItemEntity toSave = OrderItemEntity.builder()
            .pk(orderPk)
            .sk("ITEM#%03d".formatted(itemIndex.getAndIncrement()))
            .entityType("ORDER_ITEM")
            .quantity(item.getQuantity())
            .productId(item.getProductId())
            .build();
        
            orderItemEntityRepository.save(toSave);
            items.add(toSave);
        });

        return items;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Items are required");
        }
    }

}