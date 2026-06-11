package com.fredfmelo.orderservice.order.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@DynamoDbBean
public class OrderEntity {

    public static final String CUSTOMER_ORDERS_INDEX = "customer-orders-index";

    private String pk;
    private String sk;

    private UUID customerId;
    private OrderStatus status;
    private Instant createdAt;

    @DynamoDbPartitionKey
    public String getPk() {
        return pk;
    }

    @DynamoDbSortKey
    public String getSk() {
        return sk;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = CUSTOMER_ORDERS_INDEX)
    public UUID getCustomerId() {
        return customerId;
    }

    @DynamoDbSecondarySortKey(indexNames = CUSTOMER_ORDERS_INDEX)
    public Instant getCreatedAt() {
        return createdAt;
    }
}