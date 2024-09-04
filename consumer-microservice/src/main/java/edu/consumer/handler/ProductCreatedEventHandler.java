package edu.consumer.handler;

import edu.consumer.entity.ProcessedEventEntity;
import edu.consumer.exception.NotRetryableException;
import edu.consumer.exception.RetryableException;
import edu.consumer.repostory.ProcessedEventRepository;
import event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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

    private static final String URL = "http://localhost:8095/response/200";

    private final RestTemplate restTemplate;
    private final ProcessedEventRepository eventRepository;

    @KafkaHandler
    @Transactional
    public void handle(@Payload ProductCreatedEvent productCreatedEvent,
                       @Header("messageId") String messageId,
                       @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

        log.info("Received event: {}", productCreatedEvent.getTitle());

        var eventEntity = eventRepository.findByMessageId(messageId);

        if (eventEntity != null) {
            log.info("Duplicate message id: {}", messageId);
            return;
        }

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

        try {
            eventRepository.save(new ProcessedEventEntity(messageId, productCreatedEvent.getProductId()));
        } catch (DataIntegrityViolationException ex) {
            log.error(ex.getMessage());
            throw new NotRetryableException(ex);
        }
    }
}
