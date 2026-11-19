package home.inventory.shopping.copy;

import home.inventory.shopping.config.Topics;
import home.inventory.shopping.copy.dto.ArticleEventDto;
import home.inventory.shopping.copy.dto.BatchEventDto;
import home.inventory.shopping.recipe.RecipeRequestService;
import home.inventory.shopping.recipe.dto.ShoppingRequestEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * One listener (one consumer thread) for all topics: articles, batches and recipe requests are processed strictly one after
 * another, so the stock level calculation always sees the result of the previous event.
 */
@Component
public class CopyListener {

    private final CopyService service;
    private final RecipeRequestService recipes;
    private final ObjectMapper json;

    public CopyListener(CopyService service, RecipeRequestService recipes, ObjectMapper json) {
        this.service = service;
        this.recipes = recipes;
        this.json = json;
    }

    @KafkaListener(topics = {Topics.CATALOG_ARTICLE, Topics.STOCK_BATCH, Topics.RECIPE_SHOPPING_REQUEST})
    void onEvent(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        // also called for the retry topics (see KafkaRetryConfig)
        String source = topic.endsWith(Topics.RETRY_SUFFIX) ? topic.substring(0, topic.length() - Topics.RETRY_SUFFIX.length()) : topic;
        switch (source) {
            case Topics.CATALOG_ARTICLE -> service.applyArticle(json.readValue(payload, ArticleEventDto.class));
            case Topics.STOCK_BATCH -> service.applyBatch(json.readValue(payload, BatchEventDto.class));
            default -> recipes.handle(json.readValue(payload, ShoppingRequestEventDto.class));
        }
    }
}
