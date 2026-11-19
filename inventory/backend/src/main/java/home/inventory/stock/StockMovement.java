package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.location.StorageLocation;
import home.inventory.scanner.Scanner;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

@Entity
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private @Nullable StorageLocation location;

    private @Nullable LocalDate bestBefore;

    private int delta;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    private MovementSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanner_id")
    private @Nullable Scanner scanner;

    private @Nullable String username;

    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(Article article, StorageLocation location, @Nullable LocalDate bestBefore, int delta,
                         MovementType type, Booking booking) {
        this.article = article;
        this.location = location;
        this.bestBefore = bestBefore;
        this.delta = delta;
        this.type = type;
        this.source = booking.source();
        this.scanner = booking.scanner();
        this.username = booking.username();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Article getArticle() { return article; }
    public void setArticle(Article article) { this.article = article; }
    public @Nullable StorageLocation getLocation() { return location; }
    public @Nullable LocalDate getBestBefore() { return bestBefore; }
    public int getDelta() { return delta; }
    public MovementType getType() { return type; }
    public MovementSource getSource() { return source; }
    public @Nullable Scanner getScanner() { return scanner; }
    public @Nullable String getUsername() { return username; }
    public Instant getCreatedAt() { return createdAt; }
}
