package home.inventory.stock.movement;

import home.inventory.stock.batch.StockQueryService;
import home.inventory.stock.movement.dto.MovementPageDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/movements")
@Tag(name = "Movements")
public class MovementController {

    private final StockQueryService queries;

    MovementController(StockQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    public MovementPageDto listMovements(@RequestParam(required = false) @Nullable UUID articleId,
                                         @RequestParam(required = false) @Nullable UUID locationId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size) {
        return queries.movements(articleId, locationId, page, size);
    }
}
