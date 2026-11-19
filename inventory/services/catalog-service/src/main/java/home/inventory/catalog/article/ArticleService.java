package home.inventory.catalog.article;

import home.inventory.catalog.article.dto.ArticleDataDto;
import home.inventory.catalog.article.dto.ArticleDto;
import home.inventory.catalog.article.dto.ArticleListDto;
import home.inventory.catalog.article.dto.ArticleSaveDto;
import home.inventory.catalog.article.dto.ProductLookupDto;
import home.inventory.catalog.category.Category;
import home.inventory.catalog.category.CategoryRepository;
import home.inventory.catalog.category.CategoryService;
import home.inventory.catalog.common.ConflictException;
import home.inventory.catalog.common.EventPublisher;
import home.inventory.catalog.common.NotFoundException;
import home.inventory.catalog.common.OutgoingEvent;
import home.inventory.catalog.common.VersionCheck;
import home.inventory.catalog.config.Topics;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ArticleService {

    private final ArticleRepository repository;
    private final JdbcAggregateTemplate template;
    private final CategoryService categories;
    private final CategoryRepository categoryRepository;
    private final ProductLookupService lookup;
    private final EventPublisher events;

    public ArticleService(ArticleRepository repository, JdbcAggregateTemplate template, CategoryService categories,
                          CategoryRepository categoryRepository, ProductLookupService lookup, EventPublisher events) {
        this.repository = repository;
        this.template = template;
        this.categories = categories;
        this.categoryRepository = categoryRepository;
        this.lookup = lookup;
        this.events = events;
    }

    /** Active articles filtered by status/category (database) and a search term on name, brand or EAN. */
    @Transactional(readOnly = true)
    public List<ArticleListDto> list(@Nullable String search, @Nullable ArticleStatus status, @Nullable UUID categoryId) {
        Criteria criteria = Criteria.where("deleted").isFalse();
        if (status != null) {
            criteria = criteria.and("status").is(status);
        }
        if (categoryId != null) {
            criteria = criteria.and("categoryId").is(categoryId);
        }
        String term = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::id, Category::name));
        return template.findAll(Query.query(criteria).sort(Sort.by("name")), Article.class).stream()
                .filter(a -> term == null || matches(a, term))
                .map(a -> ArticleMapper.toListDto(a, a.categoryId() == null ? null : categoryNames.get(a.categoryId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArticleDto get(UUID id) {
        return ArticleMapper.toDto(load(id));
    }

    @Transactional(readOnly = true)
    public ArticleListDto getByEan(String ean) {
        Article article = findActiveByEan(ean.trim()).orElseThrow(() -> new NotFoundException("Artikel mit EAN", ean));
        String categoryName = article.categoryId() == null ? null
                : categories.findActive(article.categoryId()).map(Category::name).orElse(null);
        return ArticleMapper.toListDto(article, categoryName);
    }

    @Transactional(readOnly = true)
    public ProductLookupDto lookup(String ean) {
        String code = ean.trim();
        UUID existing = findActiveByEan(code).map(Article::id).orElse(null);
        return lookup.lookup(code)
                .map(i -> new ProductLookupDto(true, i.name(), i.brand(), i.packageSize(), i.imageUrl(), i.source(), existing))
                .orElseGet(() -> new ProductLookupDto(false, null, null, null, null, null, existing));
    }

    public UUID create(ArticleSaveDto request) {
        Article article = Article.create(request.data().name().trim(), ArticleStatus.ACTIVE, ArticleSource.MANUAL, Set.of());
        return save(apply(article, request.data())).id();
    }

    public void update(UUID id, ArticleSaveDto request) {
        Article current = VersionCheck.require(load(id), request.audit() == null ? null : request.audit().version());
        save(apply(current, request.data()));
    }

    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    /**
     * Merges an (inbox) article into another one: the barcodes move to the target, the source is soft deleted
     * and points to the target. The stock service moves stock and history when it receives the event.
     */
    public void merge(UUID id, UUID targetId, Long version) {
        if (id.equals(targetId)) {
            throw new ConflictException("Ein Artikel kann nicht mit sich selbst zusammengeführt werden");
        }
        Article source = VersionCheck.require(load(id), version);
        Article target = load(targetId);
        Set<ArticleBarcode> barcodes = new HashSet<>(target.barcodes());
        barcodes.addAll(source.barcodes());
        save(source.mergedInto(targetId));
        save(target.withBarcodes(barcodes));
    }

    /**
     * The stock service could not resolve a scanned barcode: create the article (from the product databases or
     * as inbox placeholder). If it exists already, it is published again so the stock service receives it.
     */
    public void resolvePendingBarcode(String ean) {
        String code = ean.trim();
        Optional<Article> existing = findActiveByEan(code);
        if (existing.isPresent()) {
            publish(existing.get());
            return;
        }
        Set<ArticleBarcode> barcodes = Set.of(new ArticleBarcode(code));
        Article article = lookup.lookup(code)
                .map(i -> Article.create(i.name(), ArticleStatus.ACTIVE, i.source(), barcodes)
                        .withProductInfo(i.brand(), i.packageSize(), i.imageUrl()))
                .orElseGet(() -> Article.create("Unbekannt (" + code + ")", ArticleStatus.UNKNOWN,
                        ArticleSource.PLACEHOLDER, barcodes));
        save(article);
    }

    private Article apply(Article article, ArticleDataDto data) {
        UUID categoryId = data.categoryId();
        if (categoryId != null && categories.findActive(categoryId).isEmpty()) {
            throw new NotFoundException("Kategorie", categoryId);
        }
        Set<ArticleBarcode> barcodes = new LinkedHashSet<>();
        for (String ean : data.eans()) {
            String code = ean.trim();
            findActiveByEan(code)
                    .filter(other -> !other.id().equals(article.id()))
                    .ifPresent(other -> {
                        throw new ConflictException("EAN " + code + " gehört bereits zu Artikel '" + other.name() + "'");
                    });
            barcodes.add(new ArticleBarcode(code));
        }
        return article.withData(data.name().trim(), blankToNull(data.brand()), blankToNull(data.packageSize()),
                blankToNull(data.imageUrl()), categoryId, data.minStock(), barcodes);
    }

    private Optional<Article> findActiveByEan(String ean) {
        return repository.findAllByDeletedFalse().stream().filter(a -> a.hasEan(ean)).findFirst();
    }

    private static boolean matches(Article a, String term) {
        return a.name().toLowerCase(Locale.ROOT).contains(term)
                || (a.brand() != null && a.brand().toLowerCase(Locale.ROOT).contains(term))
                || a.hasEan(term);
    }

    private Article load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Artikel", id));
    }

    private Article save(Article article) {
        Article saved = repository.save(article);
        publish(saved);
        return saved;
    }

    private void publish(Article article) {
        events.publish(new OutgoingEvent(Topics.ARTICLE, article.id(), ArticleMapper.toEventDto(article)));
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
