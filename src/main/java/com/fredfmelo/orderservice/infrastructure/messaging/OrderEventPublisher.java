package com.fredfmelo.orderservice.infrastructure.messaging;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fredfmelo.orderservice.common.exception.TechnicalException;
import com.fredfmelo.orderservice.config.ServiceConfig;
import com.fredfmelo.orderservice.outbox.publisher.OutboxEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher implements OutboxEventPublisher{

    private static final String EVENT_TYPE = "eventType";
    private static final String STRING = "String";

    private final SnsClient snsClient;
    private final ServiceConfig config;

    private Map<String, MessageAttributeValue> buildAttributes(String eventType) {
        return Map.of(EVENT_TYPE,
                MessageAttributeValue.builder()
                        .dataType(STRING)
                        .stringValue(eventType)
                        .build());
    }

	@Override
	public void publish(String payload, String eventType) {
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
}