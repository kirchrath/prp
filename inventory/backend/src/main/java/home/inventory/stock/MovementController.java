package home.inventory.stock;

import home.inventory.stock.StockDtos.MovementDto;
import home.inventory.stock.StockDtos.MovementPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
@Tag(name = "Movements")
@Transactional(readOnly = true)
public class MovementController {

    private final StockMovementRepository movements;

    MovementController(StockMovementRepository movements) {
        this.movements = movements;
    }

    @GetMapping
    public MovementPage listMovements(@RequestParam(required = false) @Nullable Long articleId,
                                      @RequestParam(required = false) @Nullable Long locationId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "50") int size) {
        Page<StockMovement> result = movements.search(articleId, locationId,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200)));
        return new MovementPage(result.map(MovementDto::of).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
