package home.inventory.article;

import home.inventory.article.ArticleDtos.ArticleRequest;
import home.inventory.category.Category;
import home.inventory.category.CategoryRepository;
import home.inventory.common.ConflictException;
import home.inventory.common.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArticleService {

    private final ArticleRepository articles;
    private final CategoryRepository categories;
    private final ProductLookupService lookup;

    ArticleService(ArticleRepository articles, CategoryRepository categories, ProductLookupService lookup) {
        this.articles = articles;
        this.categories = categories;
        this.lookup = lookup;
    }

    /** Result of resolving a scanned barcode. */
    public record Resolved(Article article, boolean created) {
    }

    @Transactional(readOnly = true)
    public List<Article> search(@Nullable String search, @Nullable ArticleStatus status, @Nullable Long categoryId) {
        String term = search == null || search.isBlank() ? null : search.trim();
        return articles.search(term, status, categoryId);
    }

    @Transactional(readOnly = true)
    public Article get(Long id) {
        return articles.findById(id).orElseThrow(() -> new NotFoundException("Artikel", id));
    }

    @Transactional(readOnly = true)
    public Optional<Article> findByEan(String ean) {
        return articles.findByEan(ean.trim());
    }

    public Article create(ArticleRequest request) {
        Article article = new Article(request.name().trim(), ArticleStatus.ACTIVE, ArticleSource.MANUAL);
        apply(article, request);
        return articles.save(article);
    }

    public Article update(Long id, ArticleRequest request) {
        Article article = get(id);
        apply(article, request);
        article.setStatus(ArticleStatus.ACTIVE);
        return article;
    }

    public void delete(Article article) {
        articles.delete(article);
    }

    public void flush() {
        articles.flush();
    }

    /**
     * Finds the article for a barcode or creates it: with data from the product databases if available,
     * otherwise as placeholder in the inbox ({@link ArticleStatus#UNKNOWN}).
     */
    public Resolved resolveOrCreate(String ean) {
        String code = ean.trim();
        Optional<Article> existing = articles.findByEan(code);
        if (existing.isPresent()) {
            return new Resolved(existing.get(), false);
        }
        Article article = lookup.lookup(code)
                .map(info -> {
                    Article a = new Article(info.name(), ArticleStatus.ACTIVE, info.source());
                    a.setBrand(info.brand());
                    a.setPackageSize(info.packageSize());
                    a.setImageUrl(info.imageUrl());
                    return a;
                })
                .orElseGet(() -> new Article("Unbekannt (" + code + ")", ArticleStatus.UNKNOWN,
                        ArticleSource.PLACEHOLDER));
        article.getEans().add(code);
        return new Resolved(articles.save(article), true);
    }

    @Transactional(readOnly = true)
    public Optional<ProductInfo> lookup(String ean) {
        return lookup.lookup(ean.trim());
    }

    private void apply(Article article, ArticleRequest request) {
        article.setName(request.name().trim());
        article.setBrand(blankToNull(request.brand()));
        article.setPackageSize(blankToNull(request.packageSize()));
        article.setImageUrl(blankToNull(request.imageUrl()));
        article.setMinStock(request.minStock());
        article.setCategory(category(request.categoryId()));
        Set<String> eans = new LinkedHashSet<>();
        for (String ean : request.eans()) {
            String code = ean.trim();
            articles.findByEan(code)
                    .filter(other -> !other.getId().equals(article.getId()))
                    .ifPresent(other -> {
                        throw new ConflictException("EAN " + code + " gehört bereits zu Artikel '" + other.getName() + "'");
                    });
            eans.add(code);
        }
        article.replaceEans(eans);
    }

    private @Nullable Category category(@Nullable Long id) {
        return id == null ? null : categories.findById(id).orElseThrow(() -> new NotFoundException("Kategorie", id));
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
