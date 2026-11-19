package home.inventory.shopping.item;

import home.inventory.shopping.item.dto.ShoppingItemDto;
import home.inventory.shopping.item.dto.ShoppingItemListDto;
import home.inventory.shopping.item.dto.ShoppingItemSaveDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping/items")
@Tag(name = "ShoppingItems")
public class ShoppingItemController {

    private final ShoppingItemService service;

    ShoppingItemController(ShoppingItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShoppingItemListDto> listShoppingItems() {
        return service.list();
    }

    @GetMapping("/{id}")
    public ShoppingItemDto getShoppingItem(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Void> createShoppingItem(@Valid @RequestBody ShoppingItemSaveDto request) {
        return ResponseEntity.created(URI.create("/api/shopping/items/" + service.create(request))).build();
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateShoppingItem(@PathVariable UUID id, @Valid @RequestBody ShoppingItemSaveDto request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShoppingItem(@PathVariable UUID id, @RequestParam Long version) {
        service.delete(id, version);
    }

    @DeleteMapping("/done")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDoneShoppingItems() {
        service.deleteDone();
    }
}
