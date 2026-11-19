package home.inventory.catalog.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** The part of a stock booking the catalog is interested in. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDataDto(@Nullable String ean, @Nullable String status) {
}
