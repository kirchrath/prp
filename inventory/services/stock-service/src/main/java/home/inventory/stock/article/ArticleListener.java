package home.inventory.stock.article;

import home.inventory.stock.article.dto.ArticleEventDto;
import home.inventory.stock.config.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ArticleListener {

    private final ArticleCopyService service;
    private final ObjectMapper json;

    public ArticleListener(ArticleCopyService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Topics.CATALOG_ARTICLE)
    void onArticle(String payload) {
        service.apply(json.readValue(payload, ArticleEventDto.class));
    }
}
