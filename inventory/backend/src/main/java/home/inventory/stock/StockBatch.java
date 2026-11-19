package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.location.StorageLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** Quantity of one article at one location with the same best-before date (or none). */
@Entity
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private StorageLocation location;

    private @Nullable LocalDate bestBefore;

    private int quantity;

    protected StockBatch() {
    }

    public StockBatch(Article article, StorageLocation location, @Nullable LocalDate bestBefore) {
        this.article = article;
        this.location = location;
        this.bestBefore = bestBefore;
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public StorageLocation getLocation() { return location; }
    public @Nullable LocalDate getBestBefore() { return bestBefore; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
