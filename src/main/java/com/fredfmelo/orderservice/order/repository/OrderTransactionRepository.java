package com.fredfmelo.orderservice.order.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.fredfmelo.eventdrivencore.outbox.entity.OutboxEntity;
import com.fredfmelo.orderservice.config.ServiceConfig;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@Repository
@RequiredArgsConstructor
public class OrderTransactionRepository {

    private final DynamoDbClient dynamoDbClient;
    private final ServiceConfig serviceConfig;

    public void save(OrderEntity order, List<OrderItemEntity> items, OutboxEntity outbox) {
        List<TransactWriteItem> transactionItems = new ArrayList<>();

        transactionItems.add(buildPut(order, OrderEntity.class));

        items.forEach(item -> transactionItems.add(buildPut(item, OrderItemEntity.class)));

        transactionItems.add(buildPut(outbox, OutboxEntity.class));

        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(transactionItems)
                .build());
    }

    private <T> TransactWriteItem buildPut(T entity, Class<T> clazz) {
        Map<String, AttributeValue> item = TableSchema.fromBean(clazz).itemToMap(entity, true);

        return TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(serviceConfig.getAws().getDynamodb().getTableName())
                        .item(item)
                        .build())
                .build();
    }
}