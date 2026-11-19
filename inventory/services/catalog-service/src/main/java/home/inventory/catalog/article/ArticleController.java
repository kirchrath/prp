package home.inventory.catalog.article;

import home.inventory.catalog.article.dto.ArticleDto;
import home.inventory.catalog.article.dto.ArticleListDto;
import home.inventory.catalog.article.dto.ArticleSaveDto;
import home.inventory.catalog.article.dto.ProductLookupDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/catalog/articles")
@Tag(name = "Articles")
public class ArticleController {

    private final ArticleService service;

    ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ArticleListDto> listArticles(@RequestParam(required = false) @Nullable String search,
                                             @RequestParam(required = false) @Nullable ArticleStatus status,
                                             @RequestParam(required = false) @Nullable UUID categoryId) {
        return service.list(search, status, categoryId);
    }

    @GetMapping("/{id}")
    public ArticleDto getArticle(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-ean/{ean}")
    public ArticleListDto getArticleByEan(@PathVariable String ean) {
        return service.getByEan(ean);
    }

    /** Looks a barcode up in the product databases without creating anything. */
    @GetMapping("/lookup/{ean}")
    public ProductLookupDto lookupProduct(@PathVariable String ean) {
        return service.lookup(ean);
    }

    @PostMapping
    public ResponseEntity<Void> createArticle(@Valid @RequestBody ArticleSaveDto request) {
        return ResponseEntity.created(URI.create("/api/catalog/articles/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateArticle(@PathVariable UUID id, @Valid @RequestBody ArticleSaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }

    @PostMapping("/{id}/merge-into/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mergeArticle(@PathVariable UUID id, @PathVariable UUID targetId, @RequestParam Long version) {
        service.merge(id, targetId, version);
    }
}
