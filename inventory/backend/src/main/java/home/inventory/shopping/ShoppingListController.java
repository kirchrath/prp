package home.inventory.shopping;

import home.inventory.article.ArticleService;
import home.inventory.common.ConflictException;
import home.inventory.common.NotFoundException;
import home.inventory.shopping.ShoppingDtos.ShoppingListItemDto;
import home.inventory.shopping.ShoppingDtos.ShoppingListItemRequest;
import home.inventory.shopping.ShoppingDtos.ShoppingListItemUpdate;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping-list")
@Tag(name = "ShoppingList")
@Transactional
public class ShoppingListController {

    private final ShoppingListItemRepository items;
    private final ArticleService articles;

    ShoppingListController(ShoppingListItemRepository items, ArticleService articles) {
        this.items = items;
        this.articles = articles;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ShoppingListItemDto> listShoppingItems() {
        return items.findAllOrdered().stream().map(ShoppingListItemDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShoppingListItemDto addShoppingItem(@Valid @RequestBody ShoppingListItemRequest request) {
        boolean noText = request.text() == null || request.text().isBlank();
        if (request.articleId() == null && noText) {
            throw new ConflictException("Artikel oder Text angeben");
        }
        var article = request.articleId() == null ? null : articles.get(request.articleId());
        var item = new ShoppingListItem(article, noText ? null : request.text().trim(), request.quantity(), false);
        return ShoppingListItemDto.of(items.save(item));
    }

    @PatchMapping("/{id}")
    public ShoppingListItemDto updateShoppingItem(@PathVariable Long id, @Valid @RequestBody ShoppingListItemUpdate update) {
        ShoppingListItem item = items.findById(id).orElseThrow(() -> new NotFoundException("Eintrag", id));
        if (update.done() != null) {
            item.setDone(update.done());
        }
        if (update.quantity() != null) {
            item.setQuantity(update.quantity());
        }
        return ShoppingListItemDto.of(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShoppingItem(@PathVariable Long id) {
        items.deleteById(id);
    }

    @DeleteMapping("/done")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDoneShoppingItems() {
        items.deleteDone();
    }
}
