package pl.panzerhund.tracker.notification.mapper;

import pl.panzerhund.tracker.listing.mapper.ListingMapper;
import pl.panzerhund.tracker.notification.dto.NotificationResponse;
import pl.panzerhund.tracker.notification.entity.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                ListingMapper.toResponse(notification.getListing()),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
