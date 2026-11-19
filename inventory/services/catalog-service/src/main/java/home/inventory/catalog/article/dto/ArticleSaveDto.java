package home.inventory.catalog.article.dto;

import home.inventory.catalog.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/** Create (without audit) or update (audit.version of the edited state). */
public record ArticleSaveDto(@Nullable AuditDto audit, @NotNull @Valid ArticleDataDto data) {
}
