package home.inventory.shopping;

import home.inventory.article.Article;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@Entity
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private @Nullable Article article;

    private @Nullable String text;

    private int quantity;

    /** Created/maintained automatically because the article fell below its minimum stock. */
    private boolean automatic;

    private boolean done;

    private Instant createdAt;

    protected ShoppingListItem() {
    }

    public ShoppingListItem(@Nullable Article article, @Nullable String text, int quantity, boolean automatic) {
        this.article = article;
        this.text = text;
        this.quantity = quantity;
        this.automatic = automatic;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public @Nullable Article getArticle() { return article; }
    public void setArticle(@Nullable Article article) { this.article = article; }
    public @Nullable String getText() { return text; }
    public void setText(@Nullable String text) { this.text = text; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isAutomatic() { return automatic; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public Instant getCreatedAt() { return createdAt; }
}
