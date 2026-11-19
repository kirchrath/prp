package home.inventory.stock.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The part of a device scan the stock service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScanDataDto(@Nullable String ean, @Nullable String direction, @Nullable UUID locationId,
                          @Nullable UUID scannerId, @Nullable String status, @Nullable UUID articleId) {
}
