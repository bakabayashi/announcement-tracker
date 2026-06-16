package pl.panzerhund.tracker.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Licznik nieprzeczytanych (polling unread-count). */
    long countByUser_IdAndReadFalse(UUID userId);

    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
