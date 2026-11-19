package home.inventory.shopping.copy;

import home.inventory.shopping.config.Topics;
import home.inventory.shopping.copy.dto.ArticleEventDto;
import home.inventory.shopping.copy.dto.BatchEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * One listener (one consumer thread) for both topics: articles and batches are processed strictly one after
 * another, so the stock level calculation always sees the result of the previous event.
 */
@Component
public class CopyListener {

    private final CopyService service;
    private final ObjectMapper json;

    public CopyListener(CopyService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = {Topics.CATALOG_ARTICLE, Topics.STOCK_BATCH})
    void onEvent(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        if (Topics.CATALOG_ARTICLE.equals(topic)) {
            service.applyArticle(json.readValue(payload, ArticleEventDto.class));
        } else {
            service.applyBatch(json.readValue(payload, BatchEventDto.class));
        }
    }
}
