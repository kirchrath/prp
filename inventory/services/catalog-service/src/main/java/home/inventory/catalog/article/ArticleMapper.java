package home.inventory.catalog.article;

import home.inventory.catalog.article.dto.ArticleDataDto;
import home.inventory.catalog.article.dto.ArticleDto;
import home.inventory.catalog.article.dto.ArticleEventDto;
import home.inventory.catalog.article.dto.ArticleListDto;
import home.inventory.catalog.common.AuditMapper;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class ArticleMapper {

    private ArticleMapper() {
    }

    public static ArticleDto toDto(Article a) {
        return new ArticleDto(AuditMapper.toDto(a), data(a));
    }

    public static ArticleEventDto toEventDto(Article a) {
        return new ArticleEventDto(a.id(), AuditMapper.toDto(a), data(a));
    }

    public static ArticleListDto toListDto(Article a, @Nullable String categoryName) {
        return new ArticleListDto(a.id(), a.name(), a.brand(), a.packageSize(), a.imageUrl(), a.categoryId(),
                categoryName, a.minStock(), a.status(), eans(a));
    }

    private static ArticleDataDto data(Article a) {
        return new ArticleDataDto(a.name(), a.brand(), a.packageSize(), a.imageUrl(), a.categoryId(), a.minStock(),
                eans(a), a.status(), a.source(), a.mergedInto());
    }

    private static List<String> eans(Article a) {
        return a.barcodes().stream().map(ArticleBarcode::ean).sorted().toList();
    }
}
