package home.inventory.catalog.article;

public enum ArticleStatus {
    /** Fully described article. */
    ACTIVE,
    /** Created from an unknown barcode; waits in the inbox for manual completion. */
    UNKNOWN
}
