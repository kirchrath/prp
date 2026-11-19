package home.inventory.catalog.common;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

/** Publishes save events to Kafka after the surrounding transaction has committed. */
@Service
public class EventPublisher {

    private final ApplicationEventPublisher applicationEvents;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public EventPublisher(ApplicationEventPublisher applicationEvents, KafkaTemplate<String, String> kafka,
                          ObjectMapper json) {
        this.applicationEvents = applicationEvents;
        this.kafka = kafka;
        this.json = json;
    }

    public void publish(OutgoingEvent event) {
        applicationEvents.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void send(OutgoingEvent event) {
        kafka.send(event.topic(), event.key().toString(), json.writeValueAsString(event.payload()));
    }
}
