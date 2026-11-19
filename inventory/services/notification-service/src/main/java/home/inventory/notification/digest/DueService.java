package home.inventory.notification.digest;

import home.inventory.notification.copy.ArticleCopy;
import home.inventory.notification.copy.ArticleCopyRepository;
import home.inventory.notification.copy.BatchCopy;
import home.inventory.notification.copy.BatchCopyRepository;
import home.inventory.notification.digest.dto.DueDto;
import home.inventory.notification.digest.dto.DueItemDto;
import home.inventory.notification.settings.NotificationSettings;
import home.inventory.notification.settings.SettingsService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Expired and expiring articles, using the threshold of the article's category (or the default). */
@Service
@Transactional(readOnly = true)
public class DueService {

    private final BatchCopyRepository batches;
    private final ArticleCopyRepository articles;
    private final SettingsService settings;

    public DueService(BatchCopyRepository batches, ArticleCopyRepository articles, SettingsService settings) {
        this.batches = batches;
        this.articles = articles;
        this.settings = settings;
    }

    private record Key(UUID articleId, LocalDate bestBefore) {
    }

    public DueDto due(LocalDate today) {
        NotificationSettings config = settings.current();
        Map<UUID, ArticleCopy> active = articles.findAllByDeletedFalse().stream()
                .collect(Collectors.toMap(ArticleCopy::articleId, Function.identity()));
        Map<Key, Integer> quantities = new LinkedHashMap<>();
        for (BatchCopy batch : batches.findDueUntil(today.plusDays(config.maxDays()))) {
            ArticleCopy article = active.get(batch.articleId());
            if (article == null) {
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, batch.bestBefore());
            int threshold = article.categoryId() == null ? config.defaultDays() : config.daysFor(article.categoryId());
            if (daysLeft <= threshold) {
                quantities.merge(new Key(batch.articleId(), batch.bestBefore()), batch.quantity(), Integer::sum);
            }
        }
        List<DueItemDto> expired = new ArrayList<>();
        List<DueItemDto> dueToday = new ArrayList<>();
        List<DueItemDto> dueSoon = new ArrayList<>();
        quantities.entrySet().stream()
                .map(e -> {
                    ArticleCopy a = active.get(e.getKey().articleId());
                    return new DueItemDto(a.articleId(), a.name(), a.brand(), e.getKey().bestBefore(), e.getValue(),
                            ChronoUnit.DAYS.between(today, e.getKey().bestBefore()));
                })
                .sorted(Comparator.comparing(DueItemDto::bestBefore).thenComparing(DueItemDto::name))
                .forEach(item -> (item.daysLeft() < 0 ? expired : item.daysLeft() == 0 ? dueToday : dueSoon).add(item));
        return new DueDto(expired, dueToday, dueSoon);
    }
}
