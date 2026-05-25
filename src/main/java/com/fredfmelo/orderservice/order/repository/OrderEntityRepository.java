package com.fredfmelo.orderservice.order.repository;

import org.springframework.stereotype.Repository;

import com.fredfmelo.orderservice.config.ServiceConfig;
import com.fredfmelo.orderservice.order.domain.OrderEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
@RequiredArgsConstructor
public class OrderEntityRepository {
    
    private final ServiceConfig serviceConfig;
    private final DynamoDbEnhancedClient client;

    private DynamoDbTable<OrderEntity> table() {

        return client.table(serviceConfig.getDynamodb().getTableName(),
                TableSchema.fromBean(OrderEntity.class));
    }

    public void save(OrderEntity entity) {
        table().putItem(entity);
    }

    public OrderEntity findByPk(String pk) {

        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue("METADATA")
                .build();

        return table().getItem(key);
    }
}