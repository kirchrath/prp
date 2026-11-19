package home.inventory.article;

import home.inventory.category.Category;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

@Entity
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private @Nullable String brand;
    private @Nullable String packageSize;
    private @Nullable String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private @Nullable Category category;

    private int minStock;

    @Enumerated(EnumType.STRING)
    private ArticleStatus status;

    @Enumerated(EnumType.STRING)
    private ArticleSource source;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "article_barcode", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "ean")
    private Set<String> eans = new TreeSet<>();

    private Instant createdAt;
    private Instant updatedAt;

    protected Article() {
    }

    public Article(String name, ArticleStatus status, ArticleSource source) {
        this.name = name;
        this.status = status;
        this.source = source;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public @Nullable String getBrand() { return brand; }
    public void setBrand(@Nullable String brand) { this.brand = brand; }
    public @Nullable String getPackageSize() { return packageSize; }
    public void setPackageSize(@Nullable String packageSize) { this.packageSize = packageSize; }
    public @Nullable String getImageUrl() { return imageUrl; }
    public void setImageUrl(@Nullable String imageUrl) { this.imageUrl = imageUrl; }
    public @Nullable Category getCategory() { return category; }
    public void setCategory(@Nullable Category category) { this.category = category; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public ArticleStatus getStatus() { return status; }
    public void setStatus(ArticleStatus status) { this.status = status; }
    public ArticleSource getSource() { return source; }
    public void setSource(ArticleSource source) { this.source = source; }
    public Set<String> getEans() { return eans; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void replaceEans(Set<String> newEans) {
        eans.clear();
        eans.addAll(newEans);
    }
}
