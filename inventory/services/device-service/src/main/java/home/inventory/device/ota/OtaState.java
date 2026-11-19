package home.inventory.device.ota;

public enum OtaState {
    /** Command sent to the device. */
    REQUESTED,
    /** Device is downloading the image. */
    DOWNLOADING,
    /** Image written and verified, device restarts. */
    INSTALLED,
    /** Device reported the new version after the restart. */
    SUCCESS,
    /** Download, checksum or installation failed, or no answer in time. */
    FAILED;

    public boolean finished() {
        return this == SUCCESS || this == FAILED;
    }
}
