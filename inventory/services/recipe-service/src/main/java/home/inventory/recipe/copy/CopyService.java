package home.inventory.recipe.copy;

import home.inventory.recipe.copy.dto.ArticleEventDto;
import home.inventory.recipe.copy.dto.BatchEventDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Local copies of catalog articles and stock batches (packages per article). */
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
            var d = event.data();
            articles.save(copy.withSource(event.audit().version(), event.audit().deleted(), d.name(), d.brand(),
                    d.contentAmount(), d.contentUnit()));
        }
    }

    public void applyBatch(BatchEventDto event) {
        BatchCopy copy = batches.findByBatchId(event.id())
                .orElseGet(() -> BatchCopy.create(event.id(), event.data().articleId()));
        if (event.audit().version() > copy.sourceVersion()) {
            batches.save(copy.withSource(event.audit().version(), event.audit().deleted(), event.data().articleId(),
                    event.data().quantity()));
        }
    }

    /** Active articles, alphabetically. */
    @Transactional(readOnly = true)
    public List<ArticleCopy> activeArticles() {
        return articles.findAllByDeletedFalseOrderByNameAsc();
    }

    /** Packages in stock per article. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> packages() {
        return batches.findAllByDeletedFalse().stream()
                .collect(Collectors.groupingBy(BatchCopy::articleId, Collectors.summingLong(BatchCopy::quantity)));
    }
}
