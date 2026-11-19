package home.inventory.shopping.item;

import home.inventory.shopping.common.AuditMapper;
import home.inventory.shopping.common.ConflictException;
import home.inventory.shopping.common.EventPublisher;
import home.inventory.shopping.common.NotFoundException;
import home.inventory.shopping.common.OutgoingEvent;
import home.inventory.shopping.common.VersionCheck;
import home.inventory.shopping.config.Topics;
import home.inventory.shopping.copy.ArticleCopy;
import home.inventory.shopping.copy.ArticleCopyRepository;
import home.inventory.shopping.copy.BatchCopy;
import home.inventory.shopping.copy.BatchCopyRepository;
import home.inventory.shopping.item.dto.ShoppingItemDataDto;
import home.inventory.shopping.item.dto.ShoppingItemDto;
import home.inventory.shopping.item.dto.ShoppingItemEventDto;
import home.inventory.shopping.item.dto.ShoppingItemListDto;
import home.inventory.shopping.item.dto.ShoppingItemSaveDto;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShoppingItemService {

    private final ShoppingItemRepository repository;
    private final ArticleCopyRepository articles;
    private final BatchCopyRepository batches;
    private final EventPublisher events;

    public ShoppingItemService(ShoppingItemRepository repository, ArticleCopyRepository articles,
                               BatchCopyRepository batches, EventPublisher events) {
        this.repository = repository;
        this.articles = articles;
        this.batches = batches;
        this.events = events;
    }

    /** Open entries first, then by creation. */
    @Transactional(readOnly = true)
    public List<ShoppingItemListDto> list() {
        Map<UUID, ArticleCopy> copies = articles.findAll().stream()
                .collect(Collectors.toMap(ArticleCopy::articleId, Function.identity()));
        return repository.findAllByDeletedFalse().stream()
                .sorted(Comparator.comparing(ShoppingItem::done).thenComparing(ShoppingItem::createdDate))
                .map(i -> {
                    ArticleCopy a = i.articleId() == null ? null : copies.get(i.articleId());
                    String label = a != null ? a.name() : i.text() != null ? i.text() : "Unbekannter Artikel";
                    return new ShoppingItemListDto(i.id(), i.articleId(), label, a == null ? null : a.brand(),
                            i.quantity(), i.automatic(), i.done(), i.note());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ShoppingItemDto get(UUID id) {
        ShoppingItem item = load(id);
        return new ShoppingItemDto(AuditMapper.toDto(item), data(item));
    }

    public UUID create(ShoppingItemSaveDto request) {
        ShoppingItemDataDto data = request.data();
        return save(ShoppingItem.create(data.articleId(), text(data), data.quantity(), false)
                .withData(data.articleId(), text(data), data.quantity(), false, blank(data.note()))).id();
    }

    public void update(UUID id, ShoppingItemSaveDto request) {
        ShoppingItem current = VersionCheck.require(load(id), request.audit() == null ? null : request.audit().version());
        ShoppingItemDataDto data = request.data();
        save(current.withData(data.articleId(), text(data), data.quantity(), data.done(), blank(data.note())));
    }

    public void delete(UUID id, Long version) {
        save(VersionCheck.require(load(id), version).withDeleted(true));
    }

    public void deleteDone() {
        repository.findAllByDoneTrueAndDeletedFalse().forEach(i -> save(i.withDeleted(true)));
    }

    /**
     * Keeps the automatic entry of an article in sync with its stock: present with the missing quantity while the
     * total stock is below the minimum stock, removed once it is reached (or the article was deleted).
     */
    public void syncAutomatic(UUID articleId) {
        Optional<ArticleCopy> article = articles.findByArticleId(articleId);
        long total = batches.findAllByArticleIdAndDeletedFalse(articleId).stream().mapToLong(BatchCopy::quantity).sum();
        long missing = article.filter(a -> !a.deleted()).map(a -> a.minStock() - total).orElse(0L);
        Optional<ShoppingItem> existing = repository.findFirstByArticleIdAndAutomaticTrueAndDeletedFalse(articleId);
        if (missing > 0) {
            ShoppingItem item = existing.orElseGet(() -> ShoppingItem.create(articleId, null, (int) missing, true));
            if (item.id() == null || item.quantity() != missing || item.done()) {
                save(item.withData(articleId, null, (int) missing, false, item.note()));
            }
        } else {
            existing.ifPresent(i -> save(i.withDeleted(true)));
        }
    }

    private static @Nullable String text(ShoppingItemDataDto data) {
        boolean noText = data.text() == null || data.text().isBlank();
        if (data.articleId() == null && noText) {
            throw new ConflictException("Artikel oder Text angeben");
        }
        return noText ? null : data.text().trim();
    }

    /**
     * Adds an entry from a recipe: an open manual entry for the same article (or the same text) is increased
     * instead of creating a duplicate.
     */
    public void addFromRecipe(@Nullable UUID articleId, String text, int quantity, String reason) {
        repository.findAllByDeletedFalse().stream()
                .filter(i -> !i.done() && !i.automatic())
                .filter(i -> articleId != null ? articleId.equals(i.articleId())
                        : i.articleId() == null && text.equalsIgnoreCase(i.text()))
                .findFirst()
                .ifPresentOrElse(i -> save(i.withMore(quantity, reason)),
                        () -> save(ShoppingItem.create(articleId, text, quantity, false).withMore(0, reason)));
    }

    private static @Nullable String blank(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ShoppingItem load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Eintrag", id));
    }

    private ShoppingItem save(ShoppingItem item) {
        ShoppingItem saved = repository.save(item);
        events.publish(new OutgoingEvent(Topics.ITEM, saved.id(),
                new ShoppingItemEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
        return saved;
    }

    private static ShoppingItemDataDto data(ShoppingItem i) {
        return new ShoppingItemDataDto(i.articleId(), i.text(), i.quantity(), i.automatic(), i.done(), i.note());
    }
}
