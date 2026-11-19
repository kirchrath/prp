package home.inventory.device.scanner;

import java.util.UUID;
import org.springframework.data.relational.core.mapping.Table;

/** Article booked by a button of a button box (buttons are numbered 1..8). */
@Table("scanner_button")
public record ButtonAssignment(int button, UUID articleId) {
}
