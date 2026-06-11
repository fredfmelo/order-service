package com.fredfmelo.orderservice.order.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.eventdrivencore.outbox.entity.OutboxEntity;
import com.fredfmelo.eventdrivencore.outbox.service.OutboxService;
import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.model.OrderItem;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;
import com.fredfmelo.orderservice.order.domain.OrderStatus;
import com.fredfmelo.orderservice.order.domain.Role;
import com.fredfmelo.orderservice.order.event.InventoryReservedEvent;
import com.fredfmelo.orderservice.order.event.OrderCreatedEvent;
import com.fredfmelo.orderservice.order.event.OrderItemEvent;
import com.fredfmelo.orderservice.order.event.PaymentApprovedEvent;
import com.fredfmelo.orderservice.order.repository.OrderEntityRepository;
import com.fredfmelo.orderservice.order.repository.OrderTransactionRepository;
import com.fredfmelo.orderservice.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCommandService {

    private static final String ENTITY_TYPE_ORDER_ITEM = "ORDER_ITEM";

    private final OrderEntityRepository orderEntityRepository;
    private final OrderTransactionRepository transactionRepository;
    private final OutboxService outboxService;

    public CreateOrderResponse createOrder(CreateOrderRequest request, UserContext userContext) {
        UUID customerId = userContext.getUserId();

        validateRequest(request, customerId);
        validateDailyOrderLimit(customerId, userContext.getRole());

        OrderEntity order = buildOrder(customerId);
        List<OrderItemEntity> items = buildItems(request.getItems(), order.getPk());

        OrderCreatedEvent event = buildOrderCreatedEvent(order, items);
        OutboxEntity outbox = outboxService.buildEntity(event);

        transactionRepository.save(order, items, outbox);
        return new CreateOrderResponse()
                .orderId(order.getPk())
                .status(com.fredfmelo.orderservice.model.OrderStatus.CREATED);
    }

    private void validateDailyOrderLimit(UUID custumerId, Role role) {
        if (role == Role.ADMIN) {
            return;
        }

        if (orderEntityRepository.countOrdersCreatedToday(custumerId) >= 5) {
            throw new BusinessException("You've exceeded the daily limit of orders placed (5)",
                    HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }

    public void approvePayment(PaymentApprovedEvent event) {
        OrderEntity order = orderEntityRepository.findByPk(event.orderId())
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND.value()));

        order.setStatus(OrderStatus.PAYMENT_APRROVED);

        orderEntityRepository.save(order);
    }

    public void complete(InventoryReservedEvent event) {
        OrderEntity order = orderEntityRepository.findByPk(event.orderId())
                .orElseThrow(() -> new BusinessException("Order not found", HttpStatus.NOT_FOUND.value()));

        order.setStatus(OrderStatus.COMPLETED);

        orderEntityRepository.save(order);
    }

    private OrderEntity buildOrder(UUID userId) {
        OrderEntity order = new OrderEntity();

        order.setPk("ORDER#" + UUID.randomUUID());
        order.setSk("METADATA");
        order.setStatus(OrderStatus.CREATED);
        order.setCustomerId(userId);
        order.setCreatedAt(Instant.now());

        return order;
    }

    private List<OrderItemEntity> buildItems(List<OrderItem> orderItems, String orderPk) {
        AtomicInteger itemIndex = new AtomicInteger(1);

        return orderItems.stream()
                .map(item -> OrderItemEntity.builder()
                        .pk(orderPk)
                        .sk("ITEM#%03d".formatted(itemIndex.getAndIncrement()))
                        .entityType(ENTITY_TYPE_ORDER_ITEM)
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private OrderCreatedEvent buildOrderCreatedEvent(
            OrderEntity order,
            List<OrderItemEntity> items) {

        List<OrderItemEvent> itemEvents = items.stream()
                .map(item -> new OrderItemEvent(
                        item.getProductId(),
                        item.getQuantity()))
                .toList();

        return new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                Instant.now(),
                order.getPk().replace("ORDER#", ""),
                order.getCustomerId(),
                itemEvents);
    }

    private void validateRequest(CreateOrderRequest request, UUID userId) {
        if (userId == null) {
            throw new BusinessException("Customer ID is required", HttpStatus.FORBIDDEN.value());
        }

        if (request.getItems().isEmpty()) {
            throw new BusinessException("Order items are required");
        }
    }
}