package home.inventory.article;

import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("select a from Article a join a.eans e where e = :ean")
    Optional<Article> findByEan(@Param("ean") String ean);

    @Query("""
            select distinct a from Article a left join a.eans e
            where (:status is null or a.status = :status)
              and (:categoryId is null or a.category.id = :categoryId)
              and (:search is null
                   or lower(a.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(coalesce(a.brand, '')) like lower(concat('%', cast(:search as string), '%'))
                   or e = :search)
            order by a.name
            """)
    List<Article> search(@Param("search") @Nullable String search,
                         @Param("status") @Nullable ArticleStatus status,
                         @Param("categoryId") @Nullable Long categoryId);

    long countByStatus(ArticleStatus status);

    List<Article> findByMinStockGreaterThan(int minStock);
}
