package home.inventory.shopping;

import home.inventory.article.Article;
import home.inventory.stock.StockBatchRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShoppingListService {

    private final ShoppingListItemRepository items;
    private final StockBatchRepository batches;

    ShoppingListService(ShoppingListItemRepository items, StockBatchRepository batches) {
        this.items = items;
        this.batches = batches;
    }

    /**
     * Keeps the automatic entry of an article in sync with its stock:
     * created/updated while total stock is below the minimum, removed once it is reached again.
     */
    public void sync(Article article) {
        long total = batches.totalQuantity(article);
        long missing = article.getMinStock() - total;
        Optional<ShoppingListItem> existing = items.findByArticleAndAutomaticTrue(article);
        if (missing > 0) {
            ShoppingListItem item = existing.orElseGet(() -> items.save(
                    new ShoppingListItem(article, null, (int) missing, true)));
            item.setQuantity((int) missing);
            item.setDone(false);
        } else {
            existing.ifPresent(items::delete);
        }
    }
}
