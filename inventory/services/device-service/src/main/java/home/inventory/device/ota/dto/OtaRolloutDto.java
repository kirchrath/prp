package home.inventory.device.ota.dto;

/** Result of "update all devices of a kind": how many updates were started. */
public record OtaRolloutDto(int started) {
}
