package com.fredfmelo.orderservice.order.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.fredfmelo.orderservice.common.exception.BusinessException;
import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.model.OrderItem;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;
import com.fredfmelo.orderservice.order.domain.OrderStatus;
import com.fredfmelo.orderservice.order.event.InventoryReservedEvent;
import com.fredfmelo.orderservice.order.event.OrderCreatedEvent;
import com.fredfmelo.orderservice.order.event.OrderItemEvent;
import com.fredfmelo.orderservice.order.event.PaymentApprovedEvent;
import com.fredfmelo.orderservice.order.repository.OrderEntityRepository;
import com.fredfmelo.orderservice.order.repository.OrderItemEntityRepository;
import com.fredfmelo.orderservice.outbox.service.OutboxService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private static final String ENTITY_TYPE_ORDER_ITEM = "ORDER_ITEM";

    private final OrderEntityRepository orderEntityRepository;
    private final OrderItemEntityRepository orderItemEntityRepository;
    private final OutboxService outboxService;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validateRequest(request);

        OrderEntity orderEntity = createAndSaveOrder(request);

        List<OrderItemEntity> items = createAndSaveOrderItem(
                request.getItems(),
                orderEntity.getPk());

        createOutboxEvent(orderEntity, items);

        return new CreateOrderResponse();
    }

    public void approvePayment(PaymentApprovedEvent event) {
        OrderEntity order = orderEntityRepository.findByPk(event.orderId());

        order.setStatus(OrderStatus.PAYMENT_APRROVED);

        orderEntityRepository.save(order);
    }

    public void complete(InventoryReservedEvent event) {
        OrderEntity order = orderEntityRepository.findByPk(event.orderId());

        order.setStatus(OrderStatus.COMPLETED);

        orderEntityRepository.save(order);
    }

    private void createOutboxEvent(OrderEntity orderEntity, List<OrderItemEntity> items) {
        List<OrderItemEvent> itemEvents = items.stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                "ORDER_CREATED",
                Instant.now(),
                orderEntity.getPk().replace("ORDER#", ""),
                orderEntity.getCustomerId(),
                itemEvents);

        outboxService.save(event.eventId().toString(),
                event.eventType(),
                event);
    }

    private OrderEntity createAndSaveOrder(CreateOrderRequest request) {
        String pk = "ORDER#" + UUID.randomUUID();

        OrderEntity entity = OrderEntity.builder()
                .pk(pk)
                .sk("METADATA")
                .status(OrderStatus.CREATED)
                .customerId(request.getCustomerId())
                .build();

        orderEntityRepository.save(entity);

        return entity;
    }

    private List<OrderItemEntity> createAndSaveOrderItem(List<OrderItem> orderItemList, String orderPk) {
        AtomicInteger itemIndex = new AtomicInteger(1);
        List<OrderItemEntity> items = new ArrayList<>();

        orderItemList.forEach(item -> {

            OrderItemEntity entity = OrderItemEntity.builder()
                    .pk(orderPk)
                    .sk("ITEM#%03d".formatted(itemIndex.getAndIncrement()))
                    .entityType(ENTITY_TYPE_ORDER_ITEM)
                    .quantity(item.getQuantity())
                    .productId(item.getProductId())
                    .build();

            orderItemEntityRepository.save(entity);

            items.add(entity);
        });

        return items;
    }

    private void validateRequest(CreateOrderRequest request) {
        if (request.getCustomerId() == null) {
            throw new BusinessException("Customer ID is required");
        }

        if (request.getItems().isEmpty()) {
            throw new BusinessException("Items are required");
        }
    }
}