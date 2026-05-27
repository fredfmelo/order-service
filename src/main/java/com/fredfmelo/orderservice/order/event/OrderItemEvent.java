package com.fredfmelo.orderservice.order.event;

import java.util.UUID;

public record OrderItemEvent(UUID productId,
                Integer quantity) {
}