package pl.panzerhund.tracker.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.notification.dto.NotificationResponse;
import pl.panzerhund.tracker.notification.entity.Notification;
import pl.panzerhund.tracker.notification.entity.NotificationType;
import pl.panzerhund.tracker.notification.mapper.NotificationMapper;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationStreamService streamService;

    @Transactional(readOnly = true)
    public List<Notification> listForUser(UUID userId) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification notification = notificationRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        notification.setRead(true); // dirty checking flushes the change
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .filter(n -> !n.isRead())
                .forEach(n -> n.setRead(true));
    }

    /**
     * Persists a notification and pushes it to the user's live streams after the
     * transaction commits, so subscribers never see a notification that rolled back.
     * Integration point for the scraper / deduplication / price-drop detectors.
     */
    @Transactional
    public Notification create(UUID userId, UUID listingId, NotificationType type) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Listing", listingId));
        User user = userRepository.getReferenceById(userId);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setListing(listing);
        notification.setType(type);
        Notification saved = notificationRepository.save(notification);

        NotificationResponse payload = NotificationMapper.toResponse(saved);
        publishAfterCommit(userId, payload);
        return saved;
    }

    private void publishAfterCommit(UUID userId, NotificationResponse payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    streamService.publish(userId, payload);
                }
            });
        } else {
            streamService.publish(userId, payload);
        }
    }
}
