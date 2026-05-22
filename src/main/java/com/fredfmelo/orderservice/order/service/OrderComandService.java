package com.fredfmelo.orderservice.order.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.model.OrderItem;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;
import com.fredfmelo.orderservice.order.domain.OrderStatus;
import com.fredfmelo.orderservice.order.repository.OrderEntityRepository;
import com.fredfmelo.orderservice.order.repository.OrderItemEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderComandService {

    private final OrderEntityRepository orderEntityRepository;
    private final OrderItemEntityRepository orderItemEntityRepository;

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        validateRequest(request);

        String orderPk = createAndSaveOrder(request);
        createAndSaveOrderItem(request.getItems(), orderPk);

        // TODO: Send order created event to SNS

        CreateOrderResponse response = new CreateOrderResponse();

        return response;
    }

    private String createAndSaveOrder(CreateOrderRequest request) {
        String pk = "ORDER#" + UUID.randomUUID();

        var orderEntity = OrderEntity.builder()
                .pk(pk)
                .sk("METADATA")
                .status(OrderStatus.CREATED)
                .customerId(request.getCustomerId())
                .build();

        orderEntityRepository.save(orderEntity);

        return pk;
    }

    private void createAndSaveOrderItem(List<OrderItem> orderItemList, String orderPk) {
        AtomicInteger itemIndex = new AtomicInteger(1);

        orderItemList.forEach(item -> {
            OrderItemEntity toSave = OrderItemEntity.builder()
            .pk(orderPk)
            .sk("ITEM#%03d".formatted(itemIndex.getAndIncrement()))
            .entityType("ORDER_ITEM")
            .quantity(item.getQuantity())
            .productId(item.getProductId())
            .build();
        
            orderItemEntityRepository.save(toSave);
        });
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