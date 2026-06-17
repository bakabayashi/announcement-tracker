package pl.panzerhund.tracker.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.panzerhund.tracker.notification.entity.NotificationType;

import java.util.UUID;

/**
 * Turns scrape outcomes into notifications for the owner of the matching search criteria.
 * Keeps {@link NotificationType} (and the notification module in general) out of the scraper:
 * the orchestrator only decides <em>that</em> something happened, this decides <em>which</em> notification.
 */
@Component
@RequiredArgsConstructor
public class ScrapeNotificationProducer {

    private final NotificationService notificationService;

    /** A listing newly discovered for one of the user's criteria. */
    public void notifyNewMatch(UUID userId, UUID listingId) {
        notificationService.create(userId, listingId, NotificationType.NEW_MATCH);
    }

    /** A re-seen listing whose price dropped. */
    public void notifyPriceDrop(UUID userId, UUID listingId) {
        notificationService.create(userId, listingId, NotificationType.PRICE_DROP);
    }
}
