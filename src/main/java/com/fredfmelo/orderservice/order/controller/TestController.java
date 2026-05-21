package com.fredfmelo.orderservice.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.fredfmelo.orderservice.api.TestApi;

@RestController
public class TestController implements TestApi {

    @Override
    public ResponseEntity<Void> test() {
        return ResponseEntity.ok().build();
    }
}