package home.inventory.recipe.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param fetchTimeout       timeout when loading recipe pages
 * @param userAgent          User-Agent for recipe pages
 * @param maxPageText        characters of a page's text handed to the AI
 * @param maxArticles        catalog articles offered to the AI for matching
 */
@ConfigurationProperties("recipe")
public record RecipeProperties(
        @DefaultValue("10s") Duration fetchTimeout,
        @DefaultValue("Mozilla/5.0 (compatible; HomeInventory/0.1)") String userAgent,
        @DefaultValue("20000") int maxPageText,
        @DefaultValue("400") int maxArticles) {
}
