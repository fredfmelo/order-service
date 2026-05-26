package com.fredfmelo.orderservice.infrastructure.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fredfmelo.orderservice.common.exception.TechnicalException;
import com.fredfmelo.orderservice.config.ServiceConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String EVENT_TYPE = "eventType";
    private static final String STRING = "String";

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final ServiceConfig config;

    public void publish(Object event, String eventType) {
        try {
            publishRaw(
                    objectMapper.writeValueAsString(event),
                    eventType);

        } catch (JsonProcessingException ex) {
            throw new TechnicalException("Error serializing event", ex);
        }
    }

    public void publishRaw(String payload, String eventType) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(config.getSns().getOrderTopicArn())
                    .message(payload)
                    .messageAttributes(buildAttributes(eventType))
                    .build();

            snsClient.publish(request);

            log.info("Published eventType={}", eventType);

        } catch (SdkException ex) {
            throw new TechnicalException("Error publishing SNS event", ex);
        }
    }

    private Map<String, MessageAttributeValue> buildAttributes(String eventType) {
        return Map.of(EVENT_TYPE,
                MessageAttributeValue.builder()
                        .dataType(STRING)
                        .stringValue(eventType)
                        .build());
    }
}