package home.inventory.scanner;

import home.inventory.location.StorageLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** An ESP32 barcode scanner; registers itself via MQTT and is assigned to a location in the UI. */
@Entity
public class Scanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;
    private @Nullable String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private @Nullable StorageLocation location;

    private boolean online;
    private @Nullable String firmwareVersion;
    private @Nullable String ipAddress;
    private @Nullable Instant lastSeen;
    private @Nullable String lastBootId;
    private @Nullable Long lastSeq;
    private Instant createdAt;

    protected Scanner() {
    }

    public Scanner(String deviceId) {
        this.deviceId = deviceId;
        this.createdAt = Instant.now();
    }

    /** Returns {@code true} if this (bootId, seq) pair was already processed (MQTT QoS 1 redelivery). */
    public boolean isDuplicate(@Nullable String bootId, @Nullable Long seq) {
        return bootId != null && seq != null && Objects.equals(bootId, lastBootId)
                && lastSeq != null && seq <= lastSeq;
    }

    public void remember(@Nullable String bootId, @Nullable Long seq) {
        this.lastBootId = bootId;
        this.lastSeq = seq;
    }

    public Long getId() { return id; }
    public String getDeviceId() { return deviceId; }
    public @Nullable String getName() { return name; }
    public void setName(@Nullable String name) { this.name = name; }
    public @Nullable StorageLocation getLocation() { return location; }
    public void setLocation(@Nullable StorageLocation location) { this.location = location; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public @Nullable String getFirmwareVersion() { return firmwareVersion; }
    public void setFirmwareVersion(@Nullable String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    public @Nullable String getIpAddress() { return ipAddress; }
    public void setIpAddress(@Nullable String ipAddress) { this.ipAddress = ipAddress; }
    public @Nullable Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(@Nullable Instant lastSeen) { this.lastSeen = lastSeen; }
    public Instant getCreatedAt() { return createdAt; }

    public String getDisplayName() {
        return name != null ? name : deviceId;
    }
}
