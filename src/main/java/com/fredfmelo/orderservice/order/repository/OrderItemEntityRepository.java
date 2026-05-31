package com.fredfmelo.orderservice.order.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.fredfmelo.orderservice.config.ServiceConfig;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@RequiredArgsConstructor
public class OrderItemEntityRepository {

    private final DynamoDbEnhancedClient client;
    private final ServiceConfig serviceConfig;

    private DynamoDbTable<OrderItemEntity> table() {
        return client.table(serviceConfig.getAws().getDynamodb().getTableName(),
                TableSchema.fromBean(OrderItemEntity.class));
    }

    public void save(OrderItemEntity entity) {
        table().putItem(entity);
    }

    public OrderItemEntity findByKey(String pk, String sk) {

        Key key = Key.builder()
                .partitionValue(pk)
                .sortValue(sk)
                .build();

        return table().getItem(key);
    }

    public List<OrderItemEntity> findByOrderId(String orderId) {
        return table()
                .query(QueryConditional.keyEqualTo(Key.builder().partitionValue("ORDER#" + orderId).build()))
                .items()
                .stream()
                .filter(item -> item.getSk().startsWith("ITEM#"))
                .toList();
    }

}