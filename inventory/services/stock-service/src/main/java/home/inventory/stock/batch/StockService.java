package home.inventory.stock.batch;

import home.inventory.stock.batch.dto.StockBatchDataDto;
import home.inventory.stock.batch.dto.StockBatchEventDto;
import home.inventory.stock.common.AuditMapper;
import home.inventory.stock.common.ConflictException;
import home.inventory.stock.common.EventPublisher;
import home.inventory.stock.common.NotFoundException;
import home.inventory.stock.common.OutgoingEvent;
import home.inventory.stock.common.VersionCheck;
import home.inventory.stock.config.Topics;
import home.inventory.stock.movement.BookingContext;
import home.inventory.stock.movement.Movement;
import home.inventory.stock.movement.MovementRepository;
import home.inventory.stock.movement.MovementSource;
import home.inventory.stock.movement.MovementType;
import home.inventory.stock.movement.dto.MovementDataDto;
import home.inventory.stock.movement.dto.MovementEventDto;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All changes of stock quantities; every change is recorded as movement. */
@Service
@Transactional(noRollbackFor = InsufficientStockException.class)
public class StockService {

    /** FIFO: earliest best-before first, batches without date last. */
    static final Comparator<StockBatch> FIFO = Comparator.comparing(StockBatch::bestBefore,
            Comparator.nullsLast(Comparator.naturalOrder()));

    private final StockBatchRepository batches;
    private final MovementRepository movements;
    private final EventPublisher events;

    public StockService(StockBatchRepository batches, MovementRepository movements, EventPublisher events) {
        this.batches = batches;
        this.movements = movements;
        this.events = events;
    }

    /** Adds units; returns the new quantity of the article at that location. */
    public long stockIn(UUID articleId, UUID locationId, int quantity, @Nullable LocalDate bestBefore,
                        BookingContext context) {
        StockBatch batch = batch(articleId, locationId, bestBefore);
        saveBatch(batch.withQuantity(batch.quantity() + quantity));
        saveMovement(Movement.create(articleId, locationId, bestBefore, quantity, MovementType.IN, context));
        return quantityAt(articleId, locationId);
    }

    /**
     * Removes units. Without {@code bestBefore} batches are consumed FIFO.
     * Returns the remaining quantity at that location.
     */
    public long stockOut(UUID articleId, UUID locationId, int quantity, @Nullable LocalDate bestBefore,
                         BookingContext context) {
        List<StockBatch> candidates = batches.findAllByArticleIdAndLocationIdAndDeletedFalse(articleId, locationId).stream()
                .filter(b -> bestBefore == null || bestBefore.equals(b.bestBefore()))
                .sorted(FIFO)
                .toList();
        int available = candidates.stream().mapToInt(StockBatch::quantity).sum();
        if (available < quantity) {
            throw new InsufficientStockException(available);
        }
        int remaining = quantity;
        for (StockBatch batch : candidates) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(remaining, batch.quantity());
            saveBatch(batch.withQuantity(batch.quantity() - take));
            saveMovement(Movement.create(articleId, locationId, batch.bestBefore(), -take, MovementType.OUT, context));
            remaining -= take;
        }
        return quantityAt(articleId, locationId);
    }

    /** Sets the counted quantity of a batch (0 = used up) and records a correction. */
    public void correct(UUID batchId, Long version, int newQuantity, BookingContext context) {
        StockBatch batch = VersionCheck.require(activeBatch(batchId), version);
        int delta = newQuantity - batch.quantity();
        if (delta == 0) {
            return;
        }
        saveBatch(batch.withQuantity(newQuantity));
        saveMovement(Movement.create(batch.articleId(), batch.locationId(), batch.bestBefore(), delta,
                MovementType.CORRECTION, context));
    }

    /** Moves units of a batch to the batch with the given best-before date (same article and location). */
    public void assignBestBefore(UUID batchId, Long version, int quantity, @Nullable LocalDate bestBefore) {
        StockBatch source = VersionCheck.require(activeBatch(batchId), version);
        if (quantity > source.quantity()) {
            throw new ConflictException("Ungültige Menge: 1 bis " + source.quantity() + " erlaubt");
        }
        if (Objects.equals(source.bestBefore(), bestBefore)) {
            return;
        }
        StockBatch target = batch(source.articleId(), source.locationId(), bestBefore);
        saveBatch(target.withQuantity(target.quantity() + quantity));
        saveBatch(source.withQuantity(source.quantity() - quantity));
    }

    /** Moves all stock of a merged article to its target article. */
    public void moveArticle(UUID sourceArticleId, UUID targetArticleId) {
        BookingContext system = new BookingContext(MovementSource.SYSTEM, null, null, null);
        for (StockBatch batch : batches.findAllByArticleIdAndDeletedFalse(sourceArticleId)) {
            saveBatch(batch.withQuantity(0));
            saveMovement(Movement.create(sourceArticleId, batch.locationId(), batch.bestBefore(), -batch.quantity(),
                    MovementType.MERGE, system));
            StockBatch target = batch(targetArticleId, batch.locationId(), batch.bestBefore());
            saveBatch(target.withQuantity(target.quantity() + batch.quantity()));
            saveMovement(Movement.create(targetArticleId, batch.locationId(), batch.bestBefore(), batch.quantity(),
                    MovementType.MERGE, system));
        }
    }

    public long quantityAt(UUID articleId, UUID locationId) {
        return batches.findAllByArticleIdAndLocationIdAndDeletedFalse(articleId, locationId).stream()
                .mapToLong(StockBatch::quantity).sum();
    }

    public StockBatch activeBatch(UUID batchId) {
        return batches.findById(batchId).filter(b -> !b.deleted())
                .orElseThrow(() -> new NotFoundException("Bestandsposten", batchId));
    }

    private StockBatch batch(UUID articleId, UUID locationId, @Nullable LocalDate bestBefore) {
        return batches.findAllByArticleIdAndLocationIdAndDeletedFalse(articleId, locationId).stream()
                .filter(b -> Objects.equals(b.bestBefore(), bestBefore))
                .findFirst()
                .orElseGet(() -> StockBatch.create(articleId, locationId, bestBefore));
    }

    private void saveBatch(StockBatch batch) {
        StockBatch saved = batches.save(batch);
        events.publish(new OutgoingEvent(Topics.BATCH, saved.id(), new StockBatchEventDto(saved.id(),
                AuditMapper.toDto(saved), new StockBatchDataDto(saved.articleId(), saved.locationId(),
                saved.bestBefore(), saved.quantity()))));
    }

    private void saveMovement(Movement movement) {
        Movement saved = movements.save(movement);
        events.publish(new OutgoingEvent(Topics.MOVEMENT, saved.id(), new MovementEventDto(saved.id(),
                AuditMapper.toDto(saved), new MovementDataDto(saved.articleId(), saved.locationId(), saved.bestBefore(),
                saved.delta(), saved.type(), saved.source(), saved.scannerId(), saved.username(), saved.bookingId()))));
    }
}
