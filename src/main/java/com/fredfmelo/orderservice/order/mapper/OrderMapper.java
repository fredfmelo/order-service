package com.fredfmelo.orderservice.order.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.fredfmelo.orderservice.model.GetOrderResponse;
import com.fredfmelo.orderservice.model.OrderItem;
import com.fredfmelo.orderservice.order.domain.OrderEntity;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", expression = "java(order.getPk().replace(\"ORDER#\", \"\"))")
    @Mapping(target = "items", source = "items")
    GetOrderResponse toResponse(
            OrderEntity order,
            List<OrderItemEntity> items);

    OrderItem toOrderItem(OrderItemEntity item);

    default OffsetDateTime map(Instant instant) {
        return instant == null
                ? null
                : instant.atOffset(ZoneOffset.UTC);
    }
}