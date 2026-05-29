package com.fredfmelo.orderservice.config;

import org.springframework.stereotype.Component;

import com.fredfmelo.eventdrivencore.config.DynamoProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ServiceDynamoProperties implements DynamoProperties {

    private final ServiceConfig serviceConfig;

    @Override
    public String tableName() {
        return serviceConfig.getDynamodb().getTableName();
    }
}