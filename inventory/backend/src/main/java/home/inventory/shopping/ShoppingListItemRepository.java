package home.inventory.shopping;

import home.inventory.article.Article;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    Optional<ShoppingListItem> findByArticleAndAutomaticTrue(Article article);

    @Query("select i from ShoppingListItem i left join fetch i.article order by i.done, i.createdAt")
    List<ShoppingListItem> findAllOrdered();

    long countByDoneFalse();

    @Modifying
    @Query("delete from ShoppingListItem i where i.done = true")
    int deleteDone();

    @Modifying
    @Query("delete from ShoppingListItem i where i.article = :article")
    void deleteByArticle(Article article);
}
