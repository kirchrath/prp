package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.location.StorageLocation;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    boolean existsByLocation(StorageLocation location);

    boolean existsByArticle(Article article);

    @Query("select b from StockBatch b where b.article = :article and b.location = :location and b.bestBefore = :bestBefore")
    Optional<StockBatch> findDated(@Param("article") Article article, @Param("location") StorageLocation location,
                                   @Param("bestBefore") LocalDate bestBefore);

    @Query("select b from StockBatch b where b.article = :article and b.location = :location and b.bestBefore is null")
    Optional<StockBatch> findUndated(@Param("article") Article article, @Param("location") StorageLocation location);

    /** FIFO order: earliest best-before first, batches without date last. */
    @Query("""
            select b from StockBatch b
            where b.article = :article and b.location = :location
            order by b.bestBefore asc nulls last, b.id asc
            """)
    List<StockBatch> findForRemoval(@Param("article") Article article, @Param("location") StorageLocation location);

    List<StockBatch> findByArticle(Article article);

    @Query("select coalesce(sum(b.quantity), 0) from StockBatch b where b.article = :article")
    long totalQuantity(@Param("article") Article article);

    @Query("select coalesce(sum(b.quantity), 0) from StockBatch b where b.article = :article and b.location = :location")
    long quantityAt(@Param("article") Article article, @Param("location") StorageLocation location);

    @Query("select b.article.id, sum(b.quantity) from StockBatch b where b.article.id in :ids group by b.article.id")
    List<Object[]> totalsByArticleIds(@Param("ids") Collection<Long> ids);

    @Query("""
            select b from StockBatch b join fetch b.article a join fetch b.location l
            where (:articleId is null or a.id = :articleId)
            order by a.name, b.bestBefore asc nulls last
            """)
    List<StockBatch> search(@Param("articleId") @Nullable Long articleId);

    @Query("""
            select b from StockBatch b join fetch b.article join fetch b.location
            where b.bestBefore is not null and b.bestBefore <= :until
            order by b.bestBefore
            """)
    List<StockBatch> expiringUntil(@Param("until") LocalDate until);

    @Query("select coalesce(sum(b.quantity), 0) from StockBatch b")
    long totalUnits();
}
