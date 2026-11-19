package home.inventory.shopping.copy;

import home.inventory.shopping.copy.dto.ArticleEventDto;
import home.inventory.shopping.copy.dto.BatchEventDto;
import home.inventory.shopping.item.ShoppingItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains the local copies of articles and batches and recalculates automatic shopping list entries. */
@Service
@Transactional
public class CopyService {

    private final ArticleCopyRepository articles;
    private final BatchCopyRepository batches;
    private final ShoppingItemService items;

    public CopyService(ArticleCopyRepository articles, BatchCopyRepository batches, ShoppingItemService items) {
        this.articles = articles;
        this.batches = batches;
        this.items = items;
    }

    public void applyArticle(ArticleEventDto event) {
        ArticleCopy copy = articles.findByArticleId(event.id()).orElseGet(() -> ArticleCopy.create(event.id()));
        if (event.audit().version() > copy.sourceVersion()) {
            articles.save(copy.withSource(event.audit().version(), event.audit().deleted(), event.data().name(),
                    event.data().brand(), event.data().minStock()));
            items.syncAutomatic(event.id());
        }
    }

    public void applyBatch(BatchEventDto event) {
        BatchCopy copy = batches.findByBatchId(event.id())
                .orElseGet(() -> BatchCopy.create(event.id(), event.data().articleId()));
        if (event.audit().version() > copy.sourceVersion()) {
            batches.save(copy.withSource(event.audit().version(), event.audit().deleted(), event.data().articleId(),
                    event.data().quantity()));
            items.syncAutomatic(event.data().articleId());
        }
    }
}
