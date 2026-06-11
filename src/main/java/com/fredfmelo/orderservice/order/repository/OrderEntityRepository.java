package com.fredfmelo.orderservice.order.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.fredfmelo.orderservice.config.ServiceConfig;
import com.fredfmelo.orderservice.order.domain.OrderEntity;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
@RequiredArgsConstructor
public class OrderEntityRepository {

    private final ServiceConfig serviceConfig;
    private final DynamoDbEnhancedClient client;

    private DynamoDbTable<OrderEntity> table() {
        return client.table(serviceConfig.getAws().getDynamodb().getTableName(),
                TableSchema.fromBean(OrderEntity.class));
    }

    public void save(OrderEntity entity) {
        table().putItem(entity);
    }

    public Optional<OrderEntity> findByPk(String orderId) {
        Key key = Key.builder()
                .partitionValue("ORDER#" + orderId)
                .sortValue("METADATA")    
                .build();
    
        return Optional.ofNullable(table().getItem(key));
    }

    public int countOrdersCreatedToday(UUID customerId) {

        Instant startOfDay = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        QueryConditional query = QueryConditional.sortBetween(
                Key.builder()
                        .partitionValue(customerId.toString())
                        .sortValue(startOfDay.toString())
                        .build(),
                Key.builder()
                        .partitionValue(customerId.toString())
                        .sortValue(endOfDay.toString())
                        .build());

        DynamoDbIndex<OrderEntity> index = table().index(OrderEntity.CUSTOMER_ORDERS_INDEX);

        return index.query(r -> r.queryConditional(query))
                .stream()
                .flatMap(page -> page.items().stream())
                .toList()
                .size();
    }

}