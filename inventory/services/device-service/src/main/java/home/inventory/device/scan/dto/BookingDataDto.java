package home.inventory.device.scan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The part of a stock booking the device service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDataDto(@Nullable UUID scanId, String status, @Nullable String articleName,
                             @Nullable Long quantityAtLocation, @Nullable String message) {
}
