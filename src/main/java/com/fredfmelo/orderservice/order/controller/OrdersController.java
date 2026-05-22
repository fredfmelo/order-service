package com.fredfmelo.orderservice.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fredfmelo.orderservice.api.OrderApi;
import com.fredfmelo.orderservice.model.CreateOrderRequest;
import com.fredfmelo.orderservice.model.CreateOrderResponse;
import com.fredfmelo.orderservice.order.service.OrderComandService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class OrdersController implements OrderApi {

    private final OrderComandService orderComandService;
    
    @Override
    public ResponseEntity<CreateOrderResponse> createOrder(CreateOrderRequest createOrderRequest) {
        return ResponseEntity.ok(orderComandService.createOrder(createOrderRequest));
    }

}
