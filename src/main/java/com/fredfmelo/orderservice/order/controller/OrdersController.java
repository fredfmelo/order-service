package com.fredfmelo.orderservice.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fredfmelo.orderservice.api.OrderApi;
import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.model.GetOrderResponse;
import com.fredfmelo.orderservice.model.OrderStatusApi;
import com.fredfmelo.orderservice.order.service.OrderCommandService;
import com.fredfmelo.orderservice.order.service.OrderQueryService;
import com.fredfmelo.orderservice.security.UserContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrdersController implements OrderApi {

    private final OrderCommandService orderComandService;
    private final OrderQueryService orderQueryService;
    private final UserContext userContext;

    @Override
    public ResponseEntity<CreateOrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        return ResponseEntity.ok(orderComandService.createOrder(createOrderRequest, userContext));
    }

    @Override
    public ResponseEntity<GetOrderResponse> getOrderById(UUID orderId){
        return ResponseEntity.ok(orderQueryService.getOrderById(orderId, userContext));
    }
    
    @Override
    public ResponseEntity<List<GetOrderResponse>> getOrders(OrderStatusApi orderStatus){
        return ResponseEntity.ok(orderQueryService.getOrders(userContext, orderStatus));
    }
}
