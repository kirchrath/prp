package home.inventory.notification.copy;

import home.inventory.notification.config.Topics;
import home.inventory.notification.copy.dto.ArticleEventDto;
import home.inventory.notification.copy.dto.BatchEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Keeps the local copies up to date; also receives the retry topics (see KafkaRetryConfig). */
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
        String source = topic.endsWith(Topics.RETRY_SUFFIX) ? topic.substring(0, topic.length() - Topics.RETRY_SUFFIX.length()) : topic;
        if (Topics.CATALOG_ARTICLE.equals(source)) {
            service.applyArticle(json.readValue(payload, ArticleEventDto.class));
        } else {
            service.applyBatch(json.readValue(payload, BatchEventDto.class));
        }
    }
}
