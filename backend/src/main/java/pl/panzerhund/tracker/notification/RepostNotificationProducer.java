package pl.panzerhund.tracker.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.panzerhund.tracker.notification.entity.NotificationType;

import java.util.UUID;

/**
 * Produces REPOSTED notifications for the deduplication module: when a new listing looks like a
 * relisting of one the user saved, the user is pointed at the fresh (reposted) listing.
 * Keeps {@link NotificationType} out of the deduplication module, mirroring {@link ScrapeNotificationProducer}.
 */
@Component
@RequiredArgsConstructor
public class RepostNotificationProducer {

    private final NotificationService notificationService;

    /** A saved listing appears to have been reposted; notify the saver about the new listing. */
    public void notifyReposted(UUID userId, UUID repostedListingId) {
        notificationService.create(userId, repostedListingId, NotificationType.REPOSTED);
    }
}
