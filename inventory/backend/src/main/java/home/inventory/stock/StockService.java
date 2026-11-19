package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.common.ConflictException;
import home.inventory.common.NotFoundException;
import home.inventory.location.StorageLocation;
import home.inventory.shopping.ShoppingListItemRepository;
import home.inventory.shopping.ShoppingListService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = InsufficientStockException.class)
public class StockService {

    private final StockBatchRepository batches;
    private final StockMovementRepository movements;
    private final ShoppingListService shoppingList;
    private final ShoppingListItemRepository shoppingItems;

    StockService(StockBatchRepository batches, StockMovementRepository movements, ShoppingListService shoppingList,
                 ShoppingListItemRepository shoppingItems) {
        this.batches = batches;
        this.movements = movements;
        this.shoppingList = shoppingList;
        this.shoppingItems = shoppingItems;
    }

    /** Adds {@code quantity} units; returns the new quantity of the article at that location. */
    public long stockIn(Article article, StorageLocation location, int quantity, @Nullable LocalDate bestBefore,
                        Booking booking) {
        requirePositive(quantity);
        StockBatch batch = batch(article, location, bestBefore);
        changeQuantity(batch, batch.getQuantity() + quantity);
        movements.save(new StockMovement(article, location, bestBefore, quantity, MovementType.IN, booking));
        shoppingList.sync(article);
        return batches.quantityAt(article, location);
    }

    /**
     * Removes {@code quantity} units. Without {@code bestBefore} the batches are consumed FIFO
     * (earliest best-before first, undated batches last). Returns the remaining quantity at that location.
     */
    public long stockOut(Article article, StorageLocation location, int quantity, @Nullable LocalDate bestBefore,
                         Booking booking) {
        requirePositive(quantity);
        List<StockBatch> candidates = batches.findForRemoval(article, location).stream()
                .filter(b -> bestBefore == null || bestBefore.equals(b.getBestBefore()))
                .toList();
        int available = candidates.stream().mapToInt(StockBatch::getQuantity).sum();
        if (available < quantity) {
            throw new InsufficientStockException("Nicht genug Bestand von '" + article.getName() + "' in "
                    + location.getPath() + " (vorhanden: " + available + ")");
        }
        int remaining = quantity;
        for (StockBatch batch : candidates) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, batch.getQuantity());
            changeQuantity(batch, batch.getQuantity() - take);
            movements.save(new StockMovement(article, location, batch.getBestBefore(), -take, MovementType.OUT, booking));
            remaining -= take;
        }
        shoppingList.sync(article);
        return batches.quantityAt(article, location);
    }

    /** Sets the counted quantity of a batch (0 removes it) and records a correction. */
    public void correct(Long batchId, int newQuantity, Booking booking) {
        if (newQuantity < 0) {
            throw new ConflictException("Menge darf nicht negativ sein");
        }
        StockBatch batch = get(batchId);
        int delta = newQuantity - batch.getQuantity();
        if (delta == 0) {
            return;
        }
        Article article = batch.getArticle();
        movements.save(new StockMovement(article, batch.getLocation(), batch.getBestBefore(), delta,
                MovementType.CORRECTION, booking));
        changeQuantity(batch, newQuantity);
        shoppingList.sync(article);
    }

    /** Moves {@code quantity} units of a batch to the batch with the given best-before date (same article/location). */
    public StockBatch assignBestBefore(Long batchId, int quantity, @Nullable LocalDate bestBefore) {
        StockBatch source = get(batchId);
        if (quantity <= 0 || quantity > source.getQuantity()) {
            throw new ConflictException("Ungültige Menge: 1 bis " + source.getQuantity() + " erlaubt");
        }
        if (Objects.equals(source.getBestBefore(), bestBefore)) {
            return source;
        }
        StockBatch target = batch(source.getArticle(), source.getLocation(), bestBefore);
        changeQuantity(target, target.getQuantity() + quantity);
        changeQuantity(source, source.getQuantity() - quantity);
        return target;
    }

    /** Merges all stock and history of {@code source} into {@code target} (used to resolve inbox entries). */
    public void mergeArticle(Article source, Article target) {
        for (StockBatch batch : batches.findByArticle(source)) {
            StockBatch into = batch(target, batch.getLocation(), batch.getBestBefore());
            changeQuantity(into, into.getQuantity() + batch.getQuantity());
            batches.delete(batch);
        }
        batches.flush();
        movements.reassign(source, target);
        shoppingItems.deleteByArticle(source);
        shoppingList.sync(target);
    }

    public void deleteArticleStockCheck(Article article) {
        if (batches.existsByArticle(article)) {
            throw new ConflictException("Artikel hat noch Bestand und kann nicht gelöscht werden");
        }
    }

    public StockBatch get(Long batchId) {
        return batches.findById(batchId).orElseThrow(() -> new NotFoundException("Bestandsposten", batchId));
    }

    private StockBatch batch(Article article, StorageLocation location, @Nullable LocalDate bestBefore) {
        var existing = bestBefore == null
                ? batches.findUndated(article, location)
                : batches.findDated(article, location, bestBefore);
        // new batches start with 0 and are only flushed after the quantity has been set
        return existing.orElseGet(() -> new StockBatch(article, location, bestBefore));
    }

    private void changeQuantity(StockBatch batch, int quantity) {
        if (quantity == 0) {
            if (batch.getId() != null) {
                batches.delete(batch);
            }
        } else {
            batch.setQuantity(quantity);
            if (batch.getId() == null) {
                batches.save(batch);
            }
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ConflictException("Menge muss größer als 0 sein");
        }
    }
}
