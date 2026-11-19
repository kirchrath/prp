package home.inventory.catalog.article;

import java.math.BigDecimal;

/** Content of one package in a base unit, e.g. 500 GRAM or 1500 MILLILITER. */
public record PackageContent(BigDecimal amount, ContentUnit unit) {
}
