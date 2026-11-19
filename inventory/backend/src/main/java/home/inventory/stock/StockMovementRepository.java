package home.inventory.stock;

import home.inventory.article.Article;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query(value = """
            select m from StockMovement m join fetch m.article left join fetch m.location left join fetch m.scanner
            where (:articleId is null or m.article.id = :articleId)
              and (:locationId is null or m.location.id = :locationId)
            order by m.createdAt desc, m.id desc
            """,
            countQuery = """
            select count(m) from StockMovement m
            where (:articleId is null or m.article.id = :articleId)
              and (:locationId is null or m.location.id = :locationId)
            """)
    Page<StockMovement> search(@Param("articleId") @Nullable Long articleId,
                               @Param("locationId") @Nullable Long locationId, Pageable pageable);

    @Modifying
    @Query("update StockMovement m set m.article = :target where m.article = :source")
    void reassign(@Param("source") Article source, @Param("target") Article target);
}
