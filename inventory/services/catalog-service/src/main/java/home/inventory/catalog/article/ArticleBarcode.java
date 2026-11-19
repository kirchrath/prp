package home.inventory.catalog.article;

import org.springframework.data.relational.core.mapping.Table;

/** Barcode (EAN/UPC/…) of an article; part of the article aggregate. */
@Table("article_barcode")
public record ArticleBarcode(String ean) {
}
