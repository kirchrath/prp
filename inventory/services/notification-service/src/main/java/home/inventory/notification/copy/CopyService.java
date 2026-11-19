package home.inventory.notification.copy;

import home.inventory.notification.copy.dto.ArticleEventDto;
import home.inventory.notification.copy.dto.BatchEventDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains the local copies of articles and batches; older versions are ignored. */
@Service
@Transactional
public class CopyService {

    private final ArticleCopyRepository articles;
    private final BatchCopyRepository batches;

    public CopyService(ArticleCopyRepository articles, BatchCopyRepository batches) {
        this.articles = articles;
        this.batches = batches;
    }

    public void applyArticle(ArticleEventDto event) {
        ArticleCopy copy = articles.findByArticleId(event.id()).orElseGet(() -> ArticleCopy.create(event.id()));
        if (event.audit().version() > copy.sourceVersion()) {
            articles.save(copy.withSource(event.audit().version(), event.audit().deleted(), event.data().name(),
                    event.data().brand(), event.data().categoryId()));
        }
    }

    public void applyBatch(BatchEventDto event) {
        BatchCopy copy = batches.findByBatchId(event.id())
                .orElseGet(() -> BatchCopy.create(event.id(), event.data().articleId()));
        if (event.audit().version() > copy.sourceVersion()) {
            batches.save(copy.withSource(event.audit().version(), event.audit().deleted(), event.data().articleId(),
                    event.data().bestBefore(), event.data().quantity()));
        }
    }
}
