package edu.consumer.handler;

import edu.consumer.event.ProductCreatedEvent;
import edu.consumer.exception.NotRetryableException;
import edu.consumer.exception.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@KafkaListener(
        topics = {"product-created-events-topic"},
        containerFactory = "kafkaListenerContainerFactory"
)
@Slf4j
public class ProductCreatedEventHandler {

    private static final String URL = "http://localhost:8095/response/500";

    private final RestTemplate restTemplate;

    @KafkaHandler
    public void handle(ProductCreatedEvent productCreatedEvent) {
        log.info("Received event: {}", productCreatedEvent.getTitle());

        try {
            var response = restTemplate.exchange(
                    URL,
                    HttpMethod.GET,
                    null,
                    String.class
            );
        } catch (ResourceAccessException ex) {
            log.error(ex.getMessage());
            throw new RetryableException(ex);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            throw new NotRetryableException(ex);
        }
    }
}
