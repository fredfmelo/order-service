package com.fredfmelo.orderservice.order.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fredfmelo.eventdrivencore.exception.BusinessException;
import com.fredfmelo.orderservice.model.GetOrderResponse;
import com.fredfmelo.orderservice.order.domain.OrderItemEntity;
import com.fredfmelo.orderservice.order.mapper.OrderMapper;
import com.fredfmelo.orderservice.order.repository.OrderEntityRepository;
import com.fredfmelo.orderservice.order.repository.OrderItemEntityRepository;
import com.fredfmelo.orderservice.security.UserContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderEntityRepository orderEntityRepository;
    private final OrderItemEntityRepository orderItemEntityRepository;
    private final OrderMapper orderMapper;

    public GetOrderResponse getOrderById(UUID orderId, UserContext userContext) {
        var orderIdString = orderId.toString();

        var orderNotFoundException = new BusinessException("Order not found", HttpStatus.NOT_FOUND.value());

        var orderfound = orderEntityRepository.findByPk(orderIdString).orElseThrow(() -> orderNotFoundException);

        if (!orderfound.getCustomerId().equals(userContext.getUserId())) {
            throw orderNotFoundException;
        }

        List<OrderItemEntity> items = orderItemEntityRepository.findByOrderId(orderIdString);

        return orderMapper.toResponse(orderfound, items);
    }

}
