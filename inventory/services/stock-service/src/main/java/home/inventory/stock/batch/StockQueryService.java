package home.inventory.stock.batch;

import home.inventory.stock.article.ArticleCopy;
import home.inventory.stock.article.ArticleCopyRepository;
import home.inventory.stock.batch.dto.ArticleLevelDto;
import home.inventory.stock.batch.dto.StockBatchDataDto;
import home.inventory.stock.batch.dto.StockBatchDto;
import home.inventory.stock.batch.dto.StockBatchListDto;
import home.inventory.stock.batch.dto.StockSummaryDto;
import home.inventory.stock.common.AuditMapper;
import home.inventory.stock.location.LocationService;
import home.inventory.stock.movement.Movement;
import home.inventory.stock.movement.dto.MovementListDto;
import home.inventory.stock.movement.dto.MovementPageDto;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read models for the UI: batches with article and location names, levels, history. */
@Service
@Transactional(readOnly = true)
public class StockQueryService {

    static final String UNKNOWN_ARTICLE = "Unbekannter Artikel";

    private final StockBatchRepository batches;
    private final ArticleCopyRepository articles;
    private final LocationService locations;
    private final StockService stock;
    private final JdbcAggregateTemplate template;

    public StockQueryService(StockBatchRepository batches, ArticleCopyRepository articles, LocationService locations,
                             StockService stock, JdbcAggregateTemplate template) {
        this.batches = batches;
        this.articles = articles;
        this.locations = locations;
        this.stock = stock;
        this.template = template;
    }

    /** Active batches, optionally filtered by article and by location including its sub-locations. */
    public List<StockBatchListDto> listBatches(@Nullable UUID articleId, @Nullable UUID locationId) {
        Criteria criteria = Criteria.where("deleted").isFalse();
        if (articleId != null) {
            criteria = criteria.and("articleId").is(articleId);
        }
        if (locationId != null) {
            criteria = criteria.and("locationId").in(locations.subtree(locationId));
        }
        return toListDtos(template.findAll(Query.query(criteria), StockBatch.class));
    }

    public StockBatchDto getBatch(UUID id) {
        StockBatch b = stock.activeBatch(id);
        return new StockBatchDto(AuditMapper.toDto(b),
                new StockBatchDataDto(b.articleId(), b.locationId(), b.bestBefore(), b.quantity()));
    }

    /** Total quantity per article (articles without stock are omitted). */
    public List<ArticleLevelDto> levels() {
        return batches.findAllByDeletedFalse().stream()
                .collect(Collectors.groupingBy(StockBatch::articleId, Collectors.summingLong(StockBatch::quantity)))
                .entrySet().stream()
                .map(e -> new ArticleLevelDto(e.getKey(), e.getValue()))
                .toList();
    }

    /** Total units and batches whose best-before date is within {@code days} (expired ones included). */
    public StockSummaryDto summary(int days) {
        List<StockBatch> active = batches.findAllByDeletedFalse();
        LocalDate until = LocalDate.now().plusDays(days);
        List<StockBatch> expiring = active.stream()
                .filter(b -> b.bestBefore() != null && !b.bestBefore().isAfter(until))
                .toList();
        return new StockSummaryDto(active.stream().mapToLong(StockBatch::quantity).sum(), toListDtos(expiring));
    }

    public MovementPageDto movements(@Nullable UUID articleId, @Nullable UUID locationId, int page, int size) {
        Criteria criteria = Criteria.where("deleted").isFalse();
        if (articleId != null) {
            criteria = criteria.and("articleId").is(articleId);
        }
        if (locationId != null) {
            criteria = criteria.and("locationId").is(locationId);
        }
        // sorting via the pageable: a sort on the query would also end up in the count query
        Page<Movement> result = template.findAll(Query.query(criteria), Movement.class,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200), Sort.by(Sort.Direction.DESC, "createdDate")));
        Map<UUID, String> names = articleNames();
        Map<UUID, String> paths = locations.paths();
        List<MovementListDto> items = result.getContent().stream()
                .map(m -> new MovementListDto(m.id(), m.createdDate(), m.articleId(), name(names, m.articleId()),
                        m.locationId(), paths.get(m.locationId()), m.bestBefore(), m.delta(), m.type(), m.source(),
                        m.scannerId(), m.username()))
                .toList();
        return new MovementPageDto(items, result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    private List<StockBatchListDto> toListDtos(List<StockBatch> list) {
        Set<UUID> ids = list.stream().map(StockBatch::articleId).collect(Collectors.toSet());
        Map<UUID, ArticleCopy> copies = articles.findAll().stream()
                .filter(a -> ids.contains(a.articleId()))
                .collect(Collectors.toMap(ArticleCopy::articleId, Function.identity()));
        Map<UUID, String> paths = locations.paths();
        return list.stream()
                .map(b -> {
                    ArticleCopy a = copies.get(b.articleId());
                    return new StockBatchListDto(b.id(), b.articleId(), a == null ? UNKNOWN_ARTICLE : a.name(),
                            a == null ? null : a.brand(), a == null ? null : a.imageUrl(), b.locationId(),
                            paths.get(b.locationId()), b.bestBefore(), b.quantity());
                })
                .sorted(Comparator.comparing(StockBatchListDto::articleName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StockBatchListDto::bestBefore, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private Map<UUID, String> articleNames() {
        return articles.findAll().stream().collect(Collectors.toMap(ArticleCopy::articleId, ArticleCopy::name));
    }

    private static String name(Map<UUID, String> names, UUID articleId) {
        return names.getOrDefault(articleId, UNKNOWN_ARTICLE);
    }
}
