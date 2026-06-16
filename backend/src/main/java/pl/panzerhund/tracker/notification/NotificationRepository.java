package pl.panzerhund.tracker.notification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.notification.entity.Notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Unread counter (unread-count endpoint). */
    long countByUser_IdAndReadFalse(UUID userId);

    /** Listing fetched eagerly to avoid N+1 when mapping the panel list. */
    @EntityGraph(attributePaths = "listing")
    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);
}
