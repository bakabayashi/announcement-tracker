package pl.panzerhund.tracker.notification.dto;

import pl.panzerhund.tracker.listing.dto.ListingResponse;
import pl.panzerhund.tracker.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        ListingResponse listing,
        NotificationType type,
        boolean read,
        Instant createdAt
) {
}
