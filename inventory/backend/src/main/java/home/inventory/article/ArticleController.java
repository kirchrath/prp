package home.inventory.article;

import home.inventory.article.ArticleDtos.ArticleDto;
import home.inventory.article.ArticleDtos.ArticleRequest;
import home.inventory.article.ArticleDtos.ProductLookupDto;
import home.inventory.common.ConflictException;
import home.inventory.common.NotFoundException;
import home.inventory.stock.StockBatchRepository;
import home.inventory.stock.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles")
@Transactional
public class ArticleController {

    private final ArticleService service;
    private final StockService stock;
    private final StockBatchRepository batches;

    ArticleController(ArticleService service, StockService stock, StockBatchRepository batches) {
        this.service = service;
        this.stock = stock;
        this.batches = batches;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ArticleDto> listArticles(@RequestParam(required = false) @Nullable String search,
                                         @RequestParam(required = false) @Nullable ArticleStatus status,
                                         @RequestParam(required = false) @Nullable Long categoryId) {
        List<Article> found = service.search(search, status, categoryId);
        Map<Long, Long> totals = new HashMap<>();
        if (!found.isEmpty()) {
            for (Object[] row : batches.totalsByArticleIds(found.stream().map(Article::getId).toList())) {
                totals.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }
        return found.stream().map(a -> ArticleDto.of(a, totals.getOrDefault(a.getId(), 0L))).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ArticleDto getArticle(@PathVariable Long id) {
        return dto(service.get(id));
    }

    @GetMapping("/by-ean/{ean}")
    @Transactional(readOnly = true)
    public ArticleDto getArticleByEan(@PathVariable String ean) {
        return dto(service.findByEan(ean).orElseThrow(() -> new NotFoundException("Artikel mit EAN", ean)));
    }

    /** Looks a barcode up in the product databases without creating anything. */
    @GetMapping("/lookup/{ean}")
    @Transactional(readOnly = true)
    public ProductLookupDto lookupProduct(@PathVariable String ean) {
        Long existing = service.findByEan(ean).map(Article::getId).orElse(null);
        return service.lookup(ean)
                .map(i -> new ProductLookupDto(true, i.name(), i.brand(), i.packageSize(), i.imageUrl(), i.source(), existing))
                .orElseGet(() -> new ProductLookupDto(false, null, null, null, null, null, existing));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleDto createArticle(@Valid @RequestBody ArticleRequest request) {
        return dto(service.create(request));
    }

    @PutMapping("/{id}")
    public ArticleDto updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return dto(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable Long id) {
        Article article = service.get(id);
        stock.deleteArticleStockCheck(article);
        service.delete(article);
    }

    /**
     * Merges an (inbox) article into an existing one: barcodes, stock and history move to the target,
     * the source article is deleted.
     */
    @PostMapping("/{id}/merge-into/{targetId}")
    public ArticleDto mergeArticle(@PathVariable Long id, @PathVariable Long targetId) {
        if (id.equals(targetId)) {
            throw new ConflictException("Ein Artikel kann nicht mit sich selbst zusammengeführt werden");
        }
        Article source = service.get(id);
        Article target = service.get(targetId);
        stock.mergeArticle(source, target);
        var eans = List.copyOf(source.getEans());
        source.getEans().clear();
        service.delete(source);
        service.flush();
        target.getEans().addAll(eans);
        return dto(target);
    }

    private ArticleDto dto(Article article) {
        return ArticleDto.of(article, article.getId() == null ? 0 : batches.totalQuantity(article));
    }
}
